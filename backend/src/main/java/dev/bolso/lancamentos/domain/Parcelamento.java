package dev.bolso.lancamentos.domain;

import dev.bolso.compartilhado.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Compra parcelada no cartão. Guarda o valor total, o número de parcelas e a data da compra; cada parcela vira um
 * lançamento. A divisão nunca perde centavo: os que sobram vão na primeira parcela (100,00 em 3 = 33,34 + 33,33 +
 * 33,33).
 */
public final class Parcelamento {

    public static final int MINIMO_PARCELAS = 2;
    public static final int MAXIMO_PARCELAS = 48;

    private final UUID id;
    private final UUID espacoId;
    private final UUID cartaoId;
    private final String descricao;
    private final Dinheiro valorTotal;
    private final int parcelas;
    private final int parcelaAtual;
    private final LocalDate dataDaCompra;
    private final long versao;

    private Parcelamento(
            UUID id, UUID espacoId, UUID cartaoId, String descricao, Dinheiro valorTotal, int parcelas, int parcelaAtual,
            LocalDate dataDaCompra, long versao) {
        this.id = id;
        this.espacoId = espacoId;
        this.cartaoId = cartaoId;
        this.descricao = descricao;
        this.valorTotal = valorTotal;
        this.parcelas = parcelas;
        this.parcelaAtual = parcelaAtual;
        this.dataDaCompra = dataDaCompra;
        this.versao = versao;
    }

    /**
     * Parcelamento novo. Confere o que é só dele (total, número de parcelas, parcela atual, cartão); descrição,
     * meta e tags são conferidas em cada parcela, pelas regras de {@link Lancamento}.
     */
    public static Parcelamento novo(UUID id, UUID espacoId, DadosDoParcelamento d) {
        Lancamento.exigir(d.valorTotal() != null && d.valorTotal().ehPositivo(), "O valor total deve ser maior que zero");
        Lancamento.exigir(d.parcelas() >= MINIMO_PARCELAS && d.parcelas() <= MAXIMO_PARCELAS,
                "Parcele de " + MINIMO_PARCELAS + " a " + MAXIMO_PARCELAS + " vezes");
        Lancamento.exigir(d.parcelaAtual() >= 1 && d.parcelaAtual() <= d.parcelas(),
                "A parcela atual deve ficar entre 1 e " + d.parcelas());
        Lancamento.exigir(centavos(d.valorTotal()) >= d.parcelas(), "Cada parcela precisa de pelo menos R$ 0,01");
        Lancamento.exigir(d.dataDaCompra() != null, "Informe a data da compra");
        Lancamento.exigir(d.cartaoId() != null, "Parcelamento só no cartão de crédito: escolha o cartão");
        var descricao = d.descricao() == null ? "" : d.descricao().trim();
        Lancamento.exigir(!descricao.isEmpty() && descricao.length() <= Lancamento.MAXIMO_DESCRICAO,
                "A descrição deve ter de 1 a " + Lancamento.MAXIMO_DESCRICAO + " caracteres");
        return new Parcelamento(id, espacoId, d.cartaoId(), descricao, d.valorTotal(), d.parcelas(), d.parcelaAtual(),
                d.dataDaCompra(), 0);
    }

    /** Remonta o que já está gravado, sem revalidar. */
    public static Parcelamento reconstituir(
            UUID id, UUID espacoId, UUID cartaoId, String descricao, Dinheiro valorTotal, int parcelas, int parcelaAtual,
            LocalDate dataDaCompra, long versao) {
        return new Parcelamento(id, espacoId, cartaoId, descricao, valorTotal, parcelas, parcelaAtual, dataDaCompra, versao);
    }

    /** As parcelas a criar: da parcela atual até a última. */
    public List<ParcelaPlanejada> parcelasACriar() {
        var total = centavos(valorTotal);
        var base = total / parcelas;
        var resto = total - base * parcelas;
        var lista = new ArrayList<ParcelaPlanejada>();
        for (var numero = parcelaAtual; numero <= parcelas; numero++) {
            var centavosDaParcela = numero == 1 ? base + resto : base;
            lista.add(new ParcelaPlanejada(
                    numero,
                    new Dinheiro(BigDecimal.valueOf(centavosDaParcela, 2)),
                    dataDaCompra.plusMonths(numero - 1L),
                    numero - 1));
        }
        return List.copyOf(lista);
    }

    /** A mesma compra com outra descrição (meta e tags vivem nas parcelas). */
    public Parcelamento comDescricao(String novaDescricao) {
        var limpa = novaDescricao == null ? "" : novaDescricao.trim();
        Lancamento.exigir(!limpa.isEmpty() && limpa.length() <= Lancamento.MAXIMO_DESCRICAO,
                "A descrição deve ter de 1 a " + Lancamento.MAXIMO_DESCRICAO + " caracteres");
        return new Parcelamento(id, espacoId, cartaoId, limpa, valorTotal, parcelas, parcelaAtual, dataDaCompra, versao);
    }

    private static long centavos(Dinheiro dinheiro) {
        return dinheiro.valor().movePointRight(2).longValueExact();
    }

    public UUID id() {
        return id;
    }

    public UUID espacoId() {
        return espacoId;
    }

    public UUID cartaoId() {
        return cartaoId;
    }

    public String descricao() {
        return descricao;
    }

    public Dinheiro valorTotal() {
        return valorTotal;
    }

    public int parcelas() {
        return parcelas;
    }

    public int parcelaAtual() {
        return parcelaAtual;
    }

    public LocalDate dataDaCompra() {
        return dataDaCompra;
    }

    public long versao() {
        return versao;
    }
}
