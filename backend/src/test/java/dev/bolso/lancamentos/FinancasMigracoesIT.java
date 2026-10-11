package dev.bolso.lancamentos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.TestcontainersConfig;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** V4 e V5 num PostgreSQL vazio: tabelas, as 6 metas da Casa e as restrições do banco. */
@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class FinancasMigracoesIT {

    static final UUID CASA = UUID.fromString("019a0000-0000-7000-8000-000000000011");
    static final UUID EMPRESA = UUID.fromString("019a0000-0000-7000-8000-000000000012");
    static final UUID META_CUSTOS_FIXOS = UUID.fromString("019a0000-0000-7000-8000-000000000101");

    @Autowired
    JdbcClient jdbc;

    @Test
    void criaAsTabelas() {
        var tabelas = jdbc.sql("SELECT table_name FROM information_schema.tables WHERE table_schema = 'finance'")
                .query(String.class)
                .list();

        assertThat(tabelas).contains("budget_goal", "tag", "transaction", "transaction_tag");
    }

    @Test
    void casaTemAsSeisMetasNaOrdemEComAsCores() {
        var metas = jdbc.sql("SELECT name || '|' || color FROM finance.budget_goal WHERE workspace_id = :w ORDER BY sort_order")
                .param("w", CASA)
                .query(String.class)
                .list();

        assertThat(metas).containsExactly(
                "Custos Fixos|#6EA8FE",
                "Conforto|#A3E06B",
                "Metas|#C79BFF",
                "Prazeres|#FF9F5A",
                "Liberdade Financeira|#FF8FB1",
                "Conhecimento|#5CD6E8");
    }

    @Test
    void empresaNaoTemMetas() {
        var quantas = jdbc.sql("SELECT count(*) FROM finance.budget_goal WHERE workspace_id = :w")
                .param("w", EMPRESA)
                .query(Long.class)
                .single();

        assertThat(quantas).isZero();
    }

    static Stream<Arguments> lancamentosQueOBancoRecusa() {
        var meta = META_CUSTOS_FIXOS.toString();
        return Stream.of(
                Arguments.of("valor zero", "OUT", "0.00", "'" + meta + "'", "'PIX'", "2026-10-01"),
                Arguments.of("valor negativo", "OUT", "-1.00", "'" + meta + "'", "'PIX'", "2026-10-01"),
                Arguments.of("direção fora da lista", "XX", "1.00", "NULL", "NULL", "2026-10-01"),
                Arguments.of("pagamento fora da lista", "OUT", "1.00", "'" + meta + "'", "'FIADO'", "2026-10-01"),
                Arguments.of("entrada com meta", "IN", "1.00", "'" + meta + "'", "NULL", "2026-10-01"),
                Arguments.of("entrada com pagamento", "IN", "1.00", "NULL", "'PIX'", "2026-10-01"),
                Arguments.of("saída sem pagamento", "OUT", "1.00", "'" + meta + "'", "NULL", "2026-10-01"),
                Arguments.of("mês de referência fora do dia 1", "OUT", "1.00", "'" + meta + "'", "'PIX'", "2026-10-15"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lancamentosQueOBancoRecusa")
    void bancoRecusaLancamentoInvalido(String motivo, String direcao, String valor, String meta, String pagamento, String mes) {
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO finance.transaction
                            (id, workspace_id, direction, amount, description, occurred_on, reference_month, goal_id, payment_method)
                        VALUES (gen_random_uuid(), :w, :d, :v, 'x', DATE '2026-10-09', DATE '%s', %s, %s)
                        """.formatted(mes, meta, pagamento))
                        .param("w", CASA)
                        .param("d", direcao)
                        .param("v", new java.math.BigDecimal(valor))
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void creditoJaEstaNaListaPermitida() {
        var linhas = jdbc.sql("""
                        INSERT INTO finance.transaction
                            (id, workspace_id, direction, amount, description, occurred_on, reference_month, goal_id, payment_method)
                        VALUES (gen_random_uuid(), :w, 'OUT', 1.00, 'x', DATE '2026-10-09', DATE '2026-10-01', :g, 'CREDIT')
                        """)
                .param("w", CASA)
                .param("g", META_CUSTOS_FIXOS)
                .update();

        assertThat(linhas).isOne();
    }

    @Test
    void lancamentoNaoUsaMetaDeOutroEspaco() {
        var metaDaEmpresa = UUID.randomUUID();
        jdbc.sql("INSERT INTO finance.budget_goal (id, workspace_id, name, color, sort_order) VALUES (:id, :w, 'Teto', '#112233', 1)")
                .param("id", metaDaEmpresa)
                .param("w", EMPRESA)
                .update();

        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO finance.transaction
                            (id, workspace_id, direction, amount, description, occurred_on, reference_month, goal_id, payment_method)
                        VALUES (gen_random_uuid(), :w, 'OUT', 1.00, 'x', DATE '2026-10-09', DATE '2026-10-01', :g, 'PIX')
                        """)
                        .param("w", CASA)
                        .param("g", metaDaEmpresa)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lancamentoNaoUsaTagDeOutroEspaco() {
        var lancamento = UUID.randomUUID();
        var tagDaEmpresa = UUID.randomUUID();
        jdbc.sql("""
                        INSERT INTO finance.transaction
                            (id, workspace_id, direction, amount, description, occurred_on, reference_month, goal_id, payment_method)
                        VALUES (:id, :w, 'OUT', 1.00, 'x', DATE '2026-10-09', DATE '2026-10-01', :g, 'PIX')
                        """)
                .param("id", lancamento)
                .param("w", CASA)
                .param("g", META_CUSTOS_FIXOS)
                .update();
        jdbc.sql("INSERT INTO finance.tag (id, workspace_id, name) VALUES (:id, :w, 'Luz')")
                .param("id", tagDaEmpresa)
                .param("w", EMPRESA)
                .update();

        // a ligação leva o espaço do lançamento (Casa); a tag é da Empresa, então a chave composta não casa
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO finance.transaction_tag (transaction_id, tag_id, workspace_id) VALUES (:t, :g, :w)")
                        .param("t", lancamento)
                        .param("g", tagDaEmpresa)
                        .param("w", CASA)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nomeDeTagNaoDiferenciaCaixaDentroDoEspaco() {
        jdbc.sql("INSERT INTO finance.tag (id, workspace_id, name) VALUES (gen_random_uuid(), :w, 'Luz')").param("w", CASA).update();

        assertThatThrownBy(() -> jdbc.sql("INSERT INTO finance.tag (id, workspace_id, name) VALUES (gen_random_uuid(), :w, 'LUZ')")
                        .param("w", CASA)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
