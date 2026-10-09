package dev.bolso.identidade;

import java.util.UUID;

/** Única porta dos outros módulos para a Identidade. */
public interface IdentidadeApi {

    /** Id do usuário que está chamando. */
    UUID usuarioAtualId();

    /**
     * Papel do usuário atual no espaço. Toda feature chama isto antes de tocar em dado de um espaço.
     *
     * @throws EspacoNaoEncontrado se o espaço não existe ou o usuário não é membro dele
     */
    Papel papelNoEspaco(UUID espacoId);
}
