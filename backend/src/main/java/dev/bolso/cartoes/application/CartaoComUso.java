package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.Cartao;
import dev.bolso.compartilhado.Dinheiro;

/** O cartão e quanto do limite está ocupado (faturas não pagas, inclusive as futuras das parcelas). */
public record CartaoComUso(Cartao cartao, Dinheiro usado) {

    /** Pode ser negativo: passar do limite não bloqueia a compra. */
    public Dinheiro disponivel() {
        return cartao.limite().subtrair(usado);
    }
}
