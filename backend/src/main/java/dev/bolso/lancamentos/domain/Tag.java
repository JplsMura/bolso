package dev.bolso.lancamentos.domain;

import java.util.UUID;

/** Etiqueta livre (Luz, Alimentação, Uber), com tipo de custo opcional. Nome único no espaço, sem diferenciar caixa. */
public record Tag(UUID id, UUID espacoId, String nome, TipoCusto tipoCusto) {

    public static final int MAXIMO_NOME = 40;

    /** Tag nova; apara o nome e confere o tamanho. */
    public static Tag nova(UUID id, UUID espacoId, String nome, TipoCusto tipoCusto) {
        var limpo = nome == null ? "" : nome.trim();
        if (limpo.isEmpty() || limpo.length() > MAXIMO_NOME) {
            throw new RegraDeNegocio("O nome da tag deve ter de 1 a " + MAXIMO_NOME + " caracteres");
        }
        return new Tag(id, espacoId, limpo, tipoCusto);
    }
}
