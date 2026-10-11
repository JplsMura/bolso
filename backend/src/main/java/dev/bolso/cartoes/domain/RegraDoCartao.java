package dev.bolso.cartoes.domain;

/** Uma regra de cartão ou fatura foi violada. A API responde 422 com a mensagem. */
public class RegraDoCartao extends RuntimeException {

    public RegraDoCartao(String mensagem) {
        super(mensagem);
    }
}
