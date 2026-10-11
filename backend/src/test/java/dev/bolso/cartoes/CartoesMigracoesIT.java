package dev.bolso.cartoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.TestcontainersConfig;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
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

/** V6 num PostgreSQL vazio: cartões, faturas, parcelamentos e as restrições novas de transaction. */
@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class CartoesMigracoesIT {

    static final String CASA = "'019a0000-0000-7000-8000-000000000011'";
    static final String EMPRESA = "'019a0000-0000-7000-8000-000000000012'";
    static final String META = "'019a0000-0000-7000-8000-000000000101'";
    static final String CARTAO_A = "'00000000-0000-7000-8000-00000000ca01'";
    static final String CARTAO_B = "'00000000-0000-7000-8000-00000000ca02'";
    static final String CARTAO_EMPRESA = "'00000000-0000-7000-8000-00000000ca03'";
    static final String FATURA_A = "'00000000-0000-7000-8000-00000000f001'";
    static final String PLANO = "'00000000-0000-7000-8000-00000000aa01'";

    static final String GASTO = """
            INSERT INTO finance.transaction (id, workspace_id, direction, amount, description, occurred_on, reference_month,
                                             goal_id, payment_method, card_id, invoice_id, installment_plan_id, installment_no)
            VALUES (gen_random_uuid(), %s, '%s', 25.00, 'x', DATE '2026-10-03', DATE '2026-10-01', %s, %s, %s, %s, %s, %s)
            """;

    @Autowired
    JdbcClient jdbc;

    void sql(String comando) {
        jdbc.sql(comando).update();
    }

    @BeforeEach
    void cadastros() {
        sql("""
                INSERT INTO finance.credit_card (id, workspace_id, name, closing_day, due_day, credit_limit) VALUES
                 (%s, %s, 'Cartão A', 25, 5, 4500.00), (%s, %s, 'Cartão B', 5, 15, 8000.00), (%s, %s, 'Cartão E', 10, 20, 1000.00)
                """.formatted(CARTAO_A, CASA, CARTAO_B, CASA, CARTAO_EMPRESA, EMPRESA));
        sql("""
                INSERT INTO finance.card_invoice (id, workspace_id, card_id, reference_month, closing_date, due_date)
                VALUES (%s, %s, %s, DATE '2026-10-01', DATE '2026-10-25', DATE '2026-11-05')
                """.formatted(FATURA_A, CASA, CARTAO_A));
        sql("""
                INSERT INTO finance.installment_plan (id, workspace_id, card_id, description, total_amount, installments, purchase_date)
                VALUES (%s, %s, %s, 'Curso', 150.00, 6, DATE '2026-10-03')
                """.formatted(PLANO, CASA, CARTAO_A));
    }

    @Test
    void criaAsTabelas() {
        var tabelas = jdbc.sql("SELECT table_name FROM information_schema.tables WHERE table_schema = 'finance'")
                .query(String.class)
                .list();

        assertThat(tabelas).contains("credit_card", "card_invoice", "installment_plan");
    }

    @Test
    void aceitaCompraNoCreditoEParcela() {
        sql(GASTO.formatted(CASA, "OUT", META, "'CREDIT'", CARTAO_A, FATURA_A, "NULL", "NULL"));
        sql(GASTO.formatted(CASA, "OUT", META, "'CREDIT'", CARTAO_A, FATURA_A, PLANO, "1"));

        assertThat(jdbc.sql("SELECT count(*) FROM finance.transaction WHERE card_id IS NOT NULL").query(Long.class).single())
                .isEqualTo(2);
    }

    @Test
    void cicloDaFaturaENomeLiberadoAoArquivar() {
        sql("UPDATE finance.card_invoice SET status = 'CLOSED', total_amount = 25.00 WHERE id = " + FATURA_A);
        sql("UPDATE finance.card_invoice SET status = 'PAID', paid_on = DATE '2026-10-30' WHERE id = " + FATURA_A);
        sql("UPDATE finance.credit_card SET archived_at = now() WHERE id = " + CARTAO_B);
        sql("INSERT INTO finance.credit_card (id, workspace_id, name, closing_day, due_day, credit_limit) VALUES (gen_random_uuid(), "
                + CASA + ", 'cartão b', 1, 10, 1.00)");

        assertThat(jdbc.sql("SELECT status FROM finance.card_invoice WHERE id = " + FATURA_A).query(String.class).single())
                .isEqualTo("PAID");
    }

    static Stream<Arguments> oQueOBancoRecusa() {
        var invoice = """
                INSERT INTO finance.card_invoice (id, workspace_id, card_id, reference_month, closing_date, due_date, status, total_amount, paid_on)
                VALUES (gen_random_uuid(), %s, %s, DATE '%s', DATE '%s', DATE '%s', '%s', %s, %s)
                """;
        var card = "INSERT INTO finance.credit_card (id, workspace_id, name, closing_day, due_day, credit_limit) VALUES (gen_random_uuid(), "
                + CASA + ", '%s', %s, 10, %s)";
        var plano = "INSERT INTO finance.installment_plan (id, workspace_id, card_id, description, total_amount, installments, first_installment, purchase_date) VALUES (gen_random_uuid(), "
                + CASA + ", %s, 'x', %s, %s, %s, DATE '2026-10-03')";
        return Stream.of(
                Arguments.of("crédito sem cartão", GASTO.formatted(CASA, "OUT", META, "'CREDIT'", "NULL", "NULL", "NULL", "NULL")),
                Arguments.of("cartão com PIX", GASTO.formatted(CASA, "OUT", META, "'PIX'", CARTAO_A, FATURA_A, "NULL", "NULL")),
                Arguments.of("entrada com cartão", GASTO.formatted(CASA, "IN", "NULL", "NULL", CARTAO_A, FATURA_A, "NULL", "NULL")),
                Arguments.of("crédito sem fatura", GASTO.formatted(CASA, "OUT", META, "'CREDIT'", CARTAO_A, "NULL", "NULL", "NULL")),
                Arguments.of("fatura de outro cartão", GASTO.formatted(CASA, "OUT", META, "'CREDIT'", CARTAO_B, FATURA_A, "NULL", "NULL")),
                Arguments.of("cartão de outro espaço", GASTO.formatted(CASA, "OUT", META, "'CREDIT'", CARTAO_EMPRESA, FATURA_A, "NULL", "NULL")),
                Arguments.of("parcela sem número", GASTO.formatted(CASA, "OUT", META, "'CREDIT'", CARTAO_A, FATURA_A, PLANO, "NULL")),
                Arguments.of("parcela no PIX", GASTO.formatted(CASA, "OUT", META, "'PIX'", "NULL", "NULL", PLANO, "2")),
                Arguments.of("duas faturas do mesmo cartão e mês",
                        invoice.formatted(CASA, CARTAO_A, "2026-10-01", "2026-10-25", "2026-11-05", "OPEN", "NULL", "NULL")),
                Arguments.of("fatura aberta com total",
                        invoice.formatted(CASA, CARTAO_A, "2026-11-01", "2026-11-25", "2026-12-05", "OPEN", "10.00", "NULL")),
                Arguments.of("fatura fechada sem total",
                        invoice.formatted(CASA, CARTAO_A, "2026-11-01", "2026-11-25", "2026-12-05", "CLOSED", "NULL", "NULL")),
                Arguments.of("fatura paga sem data",
                        invoice.formatted(CASA, CARTAO_A, "2026-11-01", "2026-11-25", "2026-12-05", "PAID", "10.00", "NULL")),
                Arguments.of("paga antes de fechar",
                        invoice.formatted(CASA, CARTAO_A, "2026-11-01", "2026-11-25", "2026-12-05", "PAID", "10.00", "DATE '2026-11-20'")),
                Arguments.of("fechamento fora do mês da fatura",
                        invoice.formatted(CASA, CARTAO_A, "2026-11-01", "2026-12-25", "2027-01-05", "OPEN", "NULL", "NULL")),
                Arguments.of("vence no dia do fechamento",
                        invoice.formatted(CASA, CARTAO_A, "2026-11-01", "2026-11-25", "2026-11-25", "OPEN", "NULL", "NULL")),
                Arguments.of("fatura de cartão de outro espaço",
                        invoice.formatted(CASA, CARTAO_EMPRESA, "2026-11-01", "2026-11-10", "2026-11-20", "OPEN", "NULL", "NULL")),
                Arguments.of("nome de cartão repetido com outra caixa", card.formatted("cartão a", 1, "1.00")),
                Arguments.of("dia de fechamento 32", card.formatted("C", 32, "1.00")),
                Arguments.of("limite zero", card.formatted("D", 3, "0.00")),
                Arguments.of("parcela atual maior que o total", plano.formatted(CARTAO_A, "100.00", 3, 4)),
                Arguments.of("menos de 1 centavo por parcela", plano.formatted(CARTAO_A, "0.05", 6, 1)),
                Arguments.of("1 parcela só", plano.formatted(CARTAO_A, "10.00", 1, 1)),
                Arguments.of("parcelamento com cartão de outro espaço", plano.formatted(CARTAO_EMPRESA, "10.00", 2, 1)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("oQueOBancoRecusa")
    void bancoRecusa(String motivo, String comando) {
        assertThatThrownBy(() -> sql(comando)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void numeroDeParcelaRepetidoERecusado() {
        sql(GASTO.formatted(CASA, "OUT", META, "'CREDIT'", CARTAO_A, FATURA_A, PLANO, "1"));

        assertThatThrownBy(() -> sql(GASTO.formatted(CASA, "OUT", META, "'CREDIT'", CARTAO_A, FATURA_A, PLANO, "1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
