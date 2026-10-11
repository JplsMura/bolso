package dev.bolso.lancamentos.domain;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.TipoEspaco;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Uma entrada ou saída de dinheiro de um espaço. As regras de formação ficam aqui, em Java puro:
 * valor positivo, descrição de 1 a 140 caracteres, meta e forma de pagamento conforme a direção
 * e o tipo do espaço, e cartão se e só se for crédito. A existência da meta, das tags e do cartão no espaço,
 * e a fatura em que o gasto cai, são conferidas pelo caso de uso.
 */
public final class Lancamento {

    public static final int MAXIMO_DESCRICAO = 140;
    public static final int MAXIMO_TAGS = 10;

    private final UUID id;
    private final UUID espacoId;
    private final DadosDoLancamento dados;
    private final UUID faturaId;
    private final Parcela parcela;
    private final long versao;

    private Lancamento(UUID id, UUID espacoId, DadosDoLancamento dados, UUID faturaId, Parcela parcela, long versao) {
        this.id = id;
        this.espacoId = espacoId;
        this.dados = dados;
        this.faturaId = faturaId;
        this.parcela = parcela;
        this.versao = versao;
    }

    /** Lançamento novo, ainda não gravado (versão 0). Lança {@link RegraDeNegocio} se algo não vale. */
    public static Lancamento novo(UUID id, UUID espacoId, TipoEspaco tipoDoEspaco, DadosDoLancamento dados) {
        return new Lancamento(id, espacoId, validar(tipoDoEspaco, dados), null, null, 0);
    }

    /**
     * Aplica uma edição, com as mesmas regras de um lançamento novo. Mantém id, espaço, versão e parcela; a fatura
     * se mantém e o caso de uso troca com {@link #naFatura} quando o gasto muda de fatura.
     */
    public Lancamento editar(TipoEspaco tipoDoEspaco, DadosDoLancamento novosDados) {
        var validos = validar(tipoDoEspaco, novosDados);
        return new Lancamento(id, espacoId, validos, validos.cartaoId() == null ? null : faturaId, parcela, versao);
    }

    /** Remonta o que já está gravado, sem revalidar. */
    public static Lancamento reconstituir(
            UUID id, UUID espacoId, DadosDoLancamento dados, UUID faturaId, Parcela parcela, long versao) {
        return new Lancamento(id, espacoId, dados, faturaId, parcela, versao);
    }

    /** Remonta um lançamento sem cartão. */
    public static Lancamento reconstituir(UUID id, UUID espacoId, DadosDoLancamento dados, long versao) {
        return reconstituir(id, espacoId, dados, null, null, versao);
    }

    /** O mesmo gasto, na fatura informada. Só gasto no cartão tem fatura. */
    public Lancamento naFatura(UUID novaFaturaId) {
        exigir(dados.cartaoId() != null, "Só gasto no cartão tem fatura");
        return new Lancamento(id, espacoId, dados, Objects.requireNonNull(novaFaturaId), parcela, versao);
    }

    /** O mesmo gasto, marcado como uma parcela. */
    public Lancamento comoParcela(Parcela novaParcela) {
        exigir(dados.cartaoId() != null, "Parcelamento só no cartão de crédito");
        return new Lancamento(id, espacoId, dados, faturaId, Objects.requireNonNull(novaParcela), versao);
    }

    /**
     * A edição mexe no total de alguma fatura? Valor, data, direção, forma de pagamento e cartão mexem; descrição,
     * meta e tags, não. Fatura fechada só aceita o que não mexe.
     */
    public boolean mexeNaFaturaEmRelacaoA(Lancamento anterior) {
        return !dados.valor().equals(anterior.dados.valor())
                || !dados.data().equals(anterior.dados.data())
                || dados.direcao() != anterior.dados.direcao()
                || dados.formaPagamento() != anterior.dados.formaPagamento()
                || !Objects.equals(dados.cartaoId(), anterior.dados.cartaoId());
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
            exigir(d.metaId() == null && d.formaPagamento() == null && d.cartaoId() == null,
                    "Entrada não tem meta, forma de pagamento nem cartão");
        } else {
            exigir(d.formaPagamento() != null, "Informe a forma de pagamento");
            if (d.formaPagamento() == FormaPagamento.CREDIT) {
                exigir(d.cartaoId() != null, "Escolha o cartão do gasto no crédito");
            } else {
                exigir(d.cartaoId() == null, "Cartão só vale para gasto no crédito");
            }
            if (tipoDoEspaco == TipoEspaco.HOME) {
                exigir(d.metaId() != null, "Informe a meta do gasto");
            } else {
                exigir(d.metaId() == null, "Gasto da Empresa não tem meta");
            }
        }
        return new DadosDoLancamento(
                d.direcao(), d.valor(), descricao, d.data(), d.metaId(), d.formaPagamento(), d.cartaoId(), tagIds);
    }

    static void exigir(boolean condicao, String mensagem) {
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

    public DadosDoLancamento dados() {
        return dados;
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

    /** O mês do lançamento, calculado a partir da data: é nele que o gasto conta no orçamento. */
    public YearMonth mesReferencia() {
        return YearMonth.from(dados.data());
    }

    public UUID metaId() {
        return dados.metaId();
    }

    public FormaPagamento formaPagamento() {
        return dados.formaPagamento();
    }

    /** Nulo fora do crédito. */
    public UUID cartaoId() {
        return dados.cartaoId();
    }

    /** Nulo fora do crédito. */
    public UUID faturaId() {
        return faturaId;
    }

    /** Nulo se não for parcela. */
    public Parcela parcela() {
        return parcela;
    }

    public boolean ehParcela() {
        return parcela != null;
    }

    public Set<UUID> tagIds() {
        return dados.tagIds();
    }

    public long versao() {
        return versao;
    }
}
