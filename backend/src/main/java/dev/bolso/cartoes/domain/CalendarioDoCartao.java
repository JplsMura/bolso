package dev.bolso.cartoes.domain;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Em qual fatura cai uma compra, e quando cada fatura fecha e vence. A fatura leva o nome do mês em que fecha.
 *
 * <ul>
 *   <li>Dia 29, 30 ou 31 vira o último dia nos meses mais curtos.
 *   <li>Compra antes do dia de fechamento cai na fatura do mesmo mês; no dia do fechamento ou depois, na do mês
 *       seguinte (o "melhor dia de compra" dos bancos).
 *   <li>O vencimento é o primeiro dia de vencimento depois do fechamento: no mesmo mês se o dia de vencimento vem
 *       depois, senão no mês seguinte.
 * </ul>
 */
public record CalendarioDoCartao(int diaFechamento, int diaVencimento) {

    public CalendarioDoCartao {
        if (diaFechamento < 1 || diaFechamento > 31 || diaVencimento < 1 || diaVencimento > 31) {
            throw new RegraDoCartao("Os dias de fechamento e de vencimento vão de 1 a 31");
        }
    }

    public YearMonth mesDaFatura(LocalDate compra) {
        var mes = YearMonth.from(compra);
        return compra.isBefore(fechamento(mes)) ? mes : mes.plusMonths(1);
    }

    public LocalDate fechamento(YearMonth mesDaFatura) {
        return noDia(mesDaFatura, diaFechamento);
    }

    public LocalDate vencimento(YearMonth mesDaFatura) {
        var fechamento = fechamento(mesDaFatura);
        var noMesmoMes = noDia(mesDaFatura, diaVencimento);
        return noMesmoMes.isAfter(fechamento) ? noMesmoMes : noDia(mesDaFatura.plusMonths(1), diaVencimento);
    }

    private static LocalDate noDia(YearMonth mes, int dia) {
        return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
    }
}
