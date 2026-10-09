package dev.bolso.identidade.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.identidade.EspacoNaoEncontrado;
import dev.bolso.identidade.Papel;
import dev.bolso.identidade.TipoEspaco;
import dev.bolso.identidade.domain.Espaco;
import dev.bolso.identidade.domain.UsuarioId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarEspacosTest {

    static final UsuarioId EU = new UsuarioId(UUID.fromString("00000000-0000-7000-8000-00000000000a"));
    static final Espaco CASA = new Espaco(UUID.fromString("00000000-0000-7000-8000-0000000000c1"), TipoEspaco.HOME, "Casa", Papel.OWNER);
    static final Espaco EMPRESA = new Espaco(UUID.fromString("00000000-0000-7000-8000-0000000000c2"), TipoEspaco.COMPANY, "Empresa", Papel.OWNER);

    /** Repositório falso: só devolve o que o usuário tem; a ordem é a da lista recebida. */
    static class RepositorioFalso implements EspacosRepository {
        final UsuarioId dono;
        final List<Espaco> espacos;

        RepositorioFalso(UsuarioId dono, List<Espaco> espacos) {
            this.dono = dono;
            this.espacos = espacos;
        }

        @Override
        public List<Espaco> listarDoUsuario(UsuarioId usuario) {
            return usuario.equals(dono) ? espacos : List.of();
        }

        @Override
        public Optional<Espaco> buscarDoUsuario(UsuarioId usuario, UUID espacoId) {
            return listarDoUsuario(usuario).stream().filter(e -> e.id().equals(espacoId)).findFirst();
        }
    }

    ConsultarEspacos casoDeUso(UsuarioId quemChama, List<Espaco> doDono) {
        return new ConsultarEspacos(new RepositorioFalso(EU, doDono), () -> quemChama);
    }

    @Test
    void devolveCasaAntesDeEmpresaMesmoQueARepositorioDevolvaOInverso() {
        var casoDeUso = casoDeUso(EU, List.of(EMPRESA, CASA));

        assertThat(casoDeUso.listar()).containsExactly(CASA, EMPRESA);
    }

    @Test
    void usuarioSemVinculoRecebeListaVazia() {
        var outro = new UsuarioId(UUID.fromString("00000000-0000-7000-8000-00000000000b"));

        assertThat(casoDeUso(outro, List.of(CASA, EMPRESA)).listar()).isEmpty();
    }

    @Test
    void buscaUmEspacoDoUsuario() {
        assertThat(casoDeUso(EU, List.of(CASA, EMPRESA)).buscar(EMPRESA.id())).isEqualTo(EMPRESA);
    }

    @Test
    void espacoDeOutroUsuarioEIgualAEspacoQueNaoExiste() {
        var outro = new UsuarioId(UUID.fromString("00000000-0000-7000-8000-00000000000b"));
        var comoOutro = casoDeUso(outro, List.of(CASA));

        assertThatThrownBy(() -> comoOutro.buscar(CASA.id())).isInstanceOf(EspacoNaoEncontrado.class);
        assertThatThrownBy(() -> comoOutro.buscar(UUID.randomUUID())).isInstanceOf(EspacoNaoEncontrado.class);
    }

    @Test
    void apiPublicaDevolveOPapelOuLancaNaoEncontrado() {
        var casoDeUso = casoDeUso(EU, List.of(CASA));
        var api = new IdentidadeApiService(casoDeUso, () -> EU);

        assertThat(api.usuarioAtualId()).isEqualTo(EU.valor());
        assertThat(api.papelNoEspaco(CASA.id())).isEqualTo(Papel.OWNER);
        assertThatThrownBy(() -> api.papelNoEspaco(EMPRESA.id())).isInstanceOf(EspacoNaoEncontrado.class);
    }
}
