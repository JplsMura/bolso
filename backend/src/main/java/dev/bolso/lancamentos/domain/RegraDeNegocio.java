package dev.bolso.lancamentos.domain;

/** Uma regra de negócio foi violada. A API responde 422 com a mensagem. */
public class RegraDeNegocio extends RuntimeException {

    public RegraDeNegocio(String mensagem) {
        super(mensagem);
    }
}
