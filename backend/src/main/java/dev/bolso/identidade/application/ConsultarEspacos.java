package dev.bolso.identidade.application;

import dev.bolso.identidade.EspacoNaoEncontrado;
import dev.bolso.identidade.domain.Espaco;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Casos de uso de leitura dos espaços do usuário atual. */
@Service
public class ConsultarEspacos {

    private final EspacosRepository espacos;
    private final UsuarioAtual usuarioAtual;

    public ConsultarEspacos(EspacosRepository espacos, UsuarioAtual usuarioAtual) {
        this.espacos = espacos;
        this.usuarioAtual = usuarioAtual;
    }

    /** Casa primeiro, depois Empresa (ordem do {@code TipoEspaco}), independente da ordem da porta. */
    public List<Espaco> listar() {
        return espacos.listarDoUsuario(usuarioAtual.id()).stream()
                .sorted(Comparator.comparing(Espaco::tipo).thenComparing(Espaco::nome))
                .toList();
    }

    public Espaco buscar(UUID espacoId) {
        return espacos.buscarDoUsuario(usuarioAtual.id(), espacoId).orElseThrow(() -> new EspacoNaoEncontrado(espacoId));
    }
}
