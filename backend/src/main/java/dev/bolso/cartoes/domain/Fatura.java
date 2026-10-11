package dev.bolso.cartoes.domain;

import dev.bolso.compartilhado.Dinheiro;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Fatura de um cartão num mês (o mês em que ela fecha). O ciclo é aberta → fechada → paga, com volta por "reabrir"
 * (fechada → aberta) e "desfazer pagamento" (paga → fechada). Ao fechar, o total congela; enquanto aberta, o total
 * é a soma ao vivo dos gastos dela. Pagar não cria lançamento: os gastos já contaram no orçamento pela data da
 * compra.
 */
public final class Fatura {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UUID id;
    private final UUID espacoId;
    private final UUID cartaoId;
    private final YearMonth mes;
    private final LocalDate fechamento;
    private final LocalDate vencimento;
    private final StatusDaFatura status;
    private final Dinheiro totalCongelado;
    private final LocalDate pagaEm;
    private final long versao;

    private Fatura(
            UUID id, UUID espacoId, UUID cartaoId, YearMonth mes, LocalDate fechamento, LocalDate vencimento,
            StatusDaFatura status, Dinheiro totalCongelado, LocalDate pagaEm, long versao) {
        this.id = id;
        this.espacoId = espacoId;
        this.cartaoId = cartaoId;
        this.mes = mes;
        this.fechamento = fechamento;
        this.vencimento = vencimento;
        this.status = status;
        this.totalCongelado = totalCongelado;
        this.pagaEm = pagaEm;
        this.versao = versao;
    }

    /** Fatura nova, aberta, com as datas do calendário do cartão naquele mês. */
    public static Fatura nova(UUID id, Cartao cartao, YearMonth mes) {
        var calendario = cartao.calendario();
        return new Fatura(id, cartao.espacoId(), cartao.id(), mes, calendario.fechamento(mes), calendario.vencimento(mes),
                StatusDaFatura.OPEN, null, null, 0);
    }

    public static Fatura reconstituir(
            UUID id, UUID espacoId, UUID cartaoId, YearMonth mes, LocalDate fechamento, LocalDate vencimento,
            StatusDaFatura status, Dinheiro totalCongelado, LocalDate pagaEm, long versao) {
        return new Fatura(id, espacoId, cartaoId, mes, fechamento, vencimento, status, totalCongelado, pagaEm, versao);
    }

    /** Fecha a partir da data de fechamento, congelando a soma atual dos gastos. */
    public Fatura fechar(LocalDate hoje, Dinheiro somaAtual) {
        exigirStatus(StatusDaFatura.OPEN, "Só uma fatura aberta pode ser fechada");
        if (hoje.isBefore(fechamento)) {
            throw new RegraDoCartao("Esta fatura só pode ser fechada a partir de " + fechamento.format(DIA));
        }
        return com(StatusDaFatura.CLOSED, Objects.requireNonNull(somaAtual), null);
    }

    /** Registra o pagamento integral. A data não pode ser antes do fechamento nem depois de hoje. */
    public Fatura pagar(LocalDate data, LocalDate hoje) {
        exigirStatus(StatusDaFatura.CLOSED, "Só uma fatura fechada pode ser paga");
        if (data == null) {
            throw new RegraDoCartao("Informe a data do pagamento");
        }
        if (data.isBefore(fechamento)) {
            throw new RegraDoCartao("A data do pagamento não pode ser antes do fechamento (" + fechamento.format(DIA) + ")");
        }
        if (data.isAfter(hoje)) {
            throw new RegraDoCartao("A data do pagamento não pode ser no futuro");
        }
        return com(StatusDaFatura.PAID, totalCongelado, data);
    }

    /** Volta a aceitar compras; o total deixa de ser congelado. */
    public Fatura reabrir() {
        exigirStatus(StatusDaFatura.CLOSED, "Só uma fatura fechada pode ser reaberta");
        return com(StatusDaFatura.OPEN, null, null);
    }

    public Fatura desfazerPagamento() {
        exigirStatus(StatusDaFatura.PAID, "Só uma fatura paga pode ter o pagamento desfeito");
        return com(StatusDaFatura.CLOSED, totalCongelado, null);
    }

    /** Total a mostrar: o congelado, se fechada ou paga; senão, a soma ao vivo. */
    public Dinheiro total(Dinheiro somaAoVivo) {
        return status == StatusDaFatura.OPEN ? somaAoVivo : totalCongelado;
    }

    public boolean aberta() {
        return status == StatusDaFatura.OPEN;
    }

    private void exigirStatus(StatusDaFatura esperado, String mensagem) {
        if (status != esperado) {
            throw new RegraDoCartao(mensagem);
        }
    }

    private Fatura com(StatusDaFatura novoStatus, Dinheiro total, LocalDate novoPagamento) {
        return new Fatura(id, espacoId, cartaoId, mes, fechamento, vencimento, novoStatus, total, novoPagamento, versao);
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

    public YearMonth mes() {
        return mes;
    }

    public LocalDate fechamento() {
        return fechamento;
    }

    public LocalDate vencimento() {
        return vencimento;
    }

    public StatusDaFatura status() {
        return status;
    }

    /** Nulo enquanto aberta. */
    public Dinheiro totalCongelado() {
        return totalCongelado;
    }

    /** Nulo se não foi paga. */
    public LocalDate pagaEm() {
        return pagaEm;
    }

    public long versao() {
        return versao;
    }
}
