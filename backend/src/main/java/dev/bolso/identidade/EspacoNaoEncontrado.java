package dev.bolso.identidade;

import java.util.UUID;

/**
 * O espaço não existe ou o usuário atual não é membro dele. As duas situações são iguais de
 * propósito: quem não pertence ao espaço não descobre que ele existe. A API responde 404.
 */
public class EspacoNaoEncontrado extends RuntimeException {

    public EspacoNaoEncontrado(UUID espacoId) {
        super("Espaço não encontrado: " + espacoId);
    }
}
