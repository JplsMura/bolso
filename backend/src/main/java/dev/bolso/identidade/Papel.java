package dev.bolso.identidade;

/** Papel de um usuário num espaço. Faz parte da API pública do módulo. */
public enum Papel {
    OWNER,
    EDITOR,
    VIEWER;

    /** Só quem não é VIEWER grava. */
    public boolean podeEscrever() {
        return this != VIEWER;
    }
}
