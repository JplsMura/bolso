package dev.bolso.lancamentos.domain;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.TipoEspaco;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

/**
 * Uma entrada ou saída de dinheiro de um espaço. As regras de formação ficam aqui, em Java puro:
 * valor positivo, descrição de 1 a 140 caracteres, e meta e forma de pagamento conforme a direção
 * e o tipo do espaço. A existência da meta e das tags no espaço é conferida pelo caso de uso.
 */
public final class Lancamento {

    public static final int MAXIMO_DESCRICAO = 140;
    public static final int MAXIMO_TAGS = 10;

    private final UUID id;
    private final UUID espacoId;
    private final DadosDoLancamento dados;
    private final long versao;

    private Lancamento(UUID id, UUID espacoId, DadosDoLancamento dados, long versao) {
        this.id = id;
        this.espacoId = espacoId;
        this.dados = dados;
        this.versao = versao;
    }

    /** Lançamento novo, ainda não gravado (versão 0). Lança {@link RegraDeNegocio} se algo não vale. */
    public static Lancamento novo(UUID id, UUID espacoId, TipoEspaco tipoDoEspaco, DadosDoLancamento dados) {
        return new Lancamento(id, espacoId, validar(tipoDoEspaco, dados), 0);
    }

    /** Aplica uma edição, com as mesmas regras de um lançamento novo. Mantém id, espaço e versão. */
    public Lancamento editar(TipoEspaco tipoDoEspaco, DadosDoLancamento novosDados) {
        return new Lancamento(id, espacoId, validar(tipoDoEspaco, novosDados), versao);
    }

    /** Remonta o que já está gravado, sem revalidar. */
    public static Lancamento reconstituir(UUID id, UUID espacoId, DadosDoLancamento dados, long versao) {
        return new Lancamento(id, espacoId, dados, versao);
    }

    private static DadosDoLancamento validar(TipoEspaco tipoDoEspaco, DadosDoLancamento d) {
        exigir(d.direcao() != null, "Informe se é entrada ou saída");
        exigir(d.valor() != null && d.valor().ehPositivo(), "O valor deve ser maior que zero");
        exigir(d.data() != null, "Informe a data");
        var descricao = d.descricao() == null ? "" : d.descricao().trim();
        exigir(!descricao.isEmpty(), "Informe a descrição");
        exigir(descricao.length() <= MAXIMO_DESCRICAO, "A descrição pode ter até " + MAXIMO_DESCRICAO + " caracteres");
        var tagIds = d.tagIds() == null ? Set.<UUID>of() : Set.copyOf(d.tagIds());
        exigir(tagIds.size() <= MAXIMO_TAGS, "Use no máximo " + MAXIMO_TAGS + " tags por lançamento");

        if (d.direcao() == Direcao.IN) {
            exigir(d.metaId() == null && d.formaPagamento() == null, "Entrada não tem meta nem forma de pagamento");
        } else {
            exigir(d.formaPagamento() != null, "Informe a forma de pagamento");
            exigir(d.formaPagamento() != FormaPagamento.CREDIT, "Cartão de crédito chega com a feature de cartões");
            if (tipoDoEspaco == TipoEspaco.HOME) {
                exigir(d.metaId() != null, "Informe a meta do gasto");
            } else {
                exigir(d.metaId() == null, "Gasto da Empresa não tem meta");
            }
        }
        return new DadosDoLancamento(d.direcao(), d.valor(), descricao, d.data(), d.metaId(), d.formaPagamento(), tagIds);
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new RegraDeNegocio(mensagem);
        }
    }

    public UUID id() {
        return id;
    }

    public UUID espacoId() {
        return espacoId;
    }

    public Direcao direcao() {
        return dados.direcao();
    }

    public Dinheiro valor() {
        return dados.valor();
    }

    public String descricao() {
        return dados.descricao();
    }

    public LocalDate data() {
        return dados.data();
    }

    /** O mês do lançamento, calculado a partir da data. */
    public YearMonth mesReferencia() {
        return YearMonth.from(dados.data());
    }

    public UUID metaId() {
        return dados.metaId();
    }

    public FormaPagamento formaPagamento() {
        return dados.formaPagamento();
    }

    public Set<UUID> tagIds() {
        return dados.tagIds();
    }

    public long versao() {
        return versao;
    }
}
