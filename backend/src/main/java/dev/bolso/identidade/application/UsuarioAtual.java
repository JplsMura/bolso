package dev.bolso.identidade.application;

import dev.bolso.identidade.domain.UsuarioId;

/**
 * Porta de saída: quem está chamando. Na 002 o adaptador devolve sempre o dono local;
 * na 007 passa a ler a sessão.
 */
public interface UsuarioAtual {

    UsuarioId id();
}
