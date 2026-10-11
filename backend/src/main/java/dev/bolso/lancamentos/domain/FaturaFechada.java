package dev.bolso.lancamentos.domain;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * A mudança mexeria no total de uma fatura que já está fechada ou paga. A API responde 422 com um tipo próprio,
 * para a tela oferecer o atalho para Cartões.
 */
public class FaturaFechada extends RegraDeNegocio {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    public FaturaFechada(String nomeDoCartao, YearMonth mes) {
        super("A fatura de " + mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR) + "/" + mes.getYear() + " do "
                + nomeDoCartao + " já está fechada. Reabra a fatura em Cartões para mudar este gasto.");
    }
}
