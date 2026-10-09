package dev.bolso.identidade;

import java.util.UUID;

/** Única porta dos outros módulos para a Identidade. */
public interface IdentidadeApi {

    /** Id do usuário que está chamando. */
    UUID usuarioAtualId();

    /**
     * Papel do usuário atual no espaço. Toda feature chama isto (ou um dos métodos abaixo) antes de
     * tocar em dado de um espaço.
     *
     * @throws EspacoNaoEncontrado se o espaço não existe ou o usuário não é membro dele
     */
    Papel papelNoEspaco(UUID espacoId);

    /**
     * Tipo do espaço (Casa ou Empresa), para regras que dependem dele.
     *
     * @throws EspacoNaoEncontrado se o espaço não existe ou o usuário não é membro dele
     */
    TipoEspaco tipoDoEspaco(UUID espacoId);

    /**
     * Garante que o usuário atual pode gravar no espaço.
     *
     * @throws EspacoNaoEncontrado se o espaço não existe ou o usuário não é membro dele
     * @throws SemPermissao se o papel dele é só de leitura
     */
    void exigirPermissaoDeEscrita(UUID espacoId);
}
