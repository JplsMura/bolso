package dev.bolso.identidade.application;

import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.identidade.Papel;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Implementa a porta pública do módulo em cima dos casos de uso. */
@Service
class IdentidadeApiService implements IdentidadeApi {

    private final ConsultarEspacos consultarEspacos;
    private final UsuarioAtual usuarioAtual;

    IdentidadeApiService(ConsultarEspacos consultarEspacos, UsuarioAtual usuarioAtual) {
        this.consultarEspacos = consultarEspacos;
        this.usuarioAtual = usuarioAtual;
    }

    @Override
    public UUID usuarioAtualId() {
        return usuarioAtual.id().valor();
    }

    @Override
    public Papel papelNoEspaco(UUID espacoId) {
        return consultarEspacos.buscar(espacoId).papel();
    }
}
