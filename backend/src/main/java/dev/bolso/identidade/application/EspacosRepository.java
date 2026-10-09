package dev.bolso.identidade.application;

import dev.bolso.identidade.domain.Espaco;
import dev.bolso.identidade.domain.UsuarioId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída: espaços de um usuário. Só devolve espaços dos quais ele é membro. */
public interface EspacosRepository {

    List<Espaco> listarDoUsuario(UsuarioId usuario);

    Optional<Espaco> buscarDoUsuario(UsuarioId usuario, UUID espacoId);
}
