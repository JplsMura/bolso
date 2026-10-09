package dev.bolso.lancamentos.domain;

import dev.bolso.compartilhado.Dinheiro;

/** Entradas e saídas do mês; a sobra é a diferença e pode ser negativa. */
public record TotaisDoMes(Dinheiro entradas, Dinheiro saidas) {

    public Dinheiro sobra() {
        return entradas.subtrair(saidas);
    }
}
