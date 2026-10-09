package dev.bolso.lancamentos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.EspacoNaoEncontrado;
import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.identidade.Papel;
import dev.bolso.identidade.SemPermissao;
import dev.bolso.identidade.TipoEspaco;
import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.DadosDoLancamento;
import dev.bolso.lancamentos.domain.Direcao;
import dev.bolso.lancamentos.domain.FormaPagamento;
import dev.bolso.lancamentos.domain.Lancamento;
import dev.bolso.lancamentos.domain.LancamentoNaoEncontrado;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import dev.bolso.orcamento.OrcamentoApi;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Casos de uso de escrita com portas falsas: nada de Spring nem banco. */
class GravarLancamentosTest {

    static final UUID CASA = UUID.fromString("00000000-0000-7000-8000-0000000000c1");
    static final UUID EMPRESA = UUID.fromString("00000000-0000-7000-8000-0000000000c2");
    static final UUID META = UUID.fromString("00000000-0000-7000-8000-0000000000a1");
    static final UUID TAG = UUID.fromString("00000000-0000-7000-8000-0000000000b1");
    static final Instant AGORA = Instant.parse("2026-10-09T12:00:00Z");

    Papel papelNaCasa;
    final Set<UUID> metasDaCasa = Set.of(META);
    final Set<UUID> tagsDaCasa = Set.of(TAG);
    final Map<UUID, Lancamento> gravados = new HashMap<>();
    final List<UUID> excluidos = new ArrayList<>();
    GravarLancamentos gravar;

    @BeforeEach
    void monta() {
        papelNaCasa = Papel.OWNER;
        gravados.clear();
        excluidos.clear();

        IdentidadeApi identidade = new IdentidadeApi() {
            @Override
            public UUID usuarioAtualId() {
                return UUID.randomUUID();
            }

            @Override
            public Papel papelNoEspaco(UUID espacoId) {
                if (!espacoId.equals(CASA) && !espacoId.equals(EMPRESA)) {
                    throw new EspacoNaoEncontrado(espacoId);
                }
                return papelNaCasa;
            }

            @Override
            public TipoEspaco tipoDoEspaco(UUID espacoId) {
                papelNoEspaco(espacoId);
                return espacoId.equals(CASA) ? TipoEspaco.HOME : TipoEspaco.COMPANY;
            }

            @Override
            public void exigirPermissaoDeEscrita(UUID espacoId) {
                var papel = papelNoEspaco(espacoId);
                if (!papel.podeEscrever()) {
                    throw new SemPermissao(espacoId, papel);
                }
            }
        };
        OrcamentoApi orcamento = (espacoId, metaId) -> espacoId.equals(CASA) && metasDaCasa.contains(metaId);
        TagsRepository tags = new TagsRepository() {
            @Override
            public List<dev.bolso.lancamentos.domain.Tag> listarAtivas(UUID espacoId) {
                return List.of();
            }

            @Override
            public boolean existeNome(UUID espacoId, String nome) {
                return false;
            }

            @Override
            public void salvar(dev.bolso.lancamentos.domain.Tag tag) {}

            @Override
            public boolean todasAtivasNoEspaco(UUID espacoId, Set<UUID> tagIds) {
                return espacoId.equals(CASA) ? tagsDaCasa.containsAll(tagIds) : tagIds.isEmpty();
            }
        };
        LancamentosRepository lancamentos = new LancamentosRepository() {
            @Override
            public Optional<Lancamento> buscar(UUID espacoId, UUID id) {
                return Optional.ofNullable(gravados.get(id)).filter(l -> l.espacoId().equals(espacoId));
            }

            @Override
            public void salvar(Lancamento lancamento, Instant agora) {
                // como o JPA: a versão sobe a cada gravação de algo que já existia
                var versao = gravados.containsKey(lancamento.id()) ? gravados.get(lancamento.id()).versao() + 1 : 0;
                gravados.put(lancamento.id(), Lancamento.reconstituir(lancamento.id(), lancamento.espacoId(), dadosDe(lancamento), versao));
            }

            @Override
            public boolean excluir(UUID espacoId, UUID id, Instant agora) {
                if (buscar(espacoId, id).isEmpty()) {
                    return false;
                }
                excluidos.add(id);
                gravados.remove(id);
                return true;
            }
        };
        ConsultaDeLancamentos consulta = new ConsultaDeLancamentos() {
            @Override
            public ConsultaDoMes consultarMes(UUID espacoId, YearMonth mes, FiltrosDeLancamento filtros) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<ItemDeLancamento> buscar(UUID espacoId, UUID id) {
                return Optional.ofNullable(gravados.get(id)).map(l -> new ItemDeLancamento(
                        l.id(), l.direcao(), l.valor(), l.descricao(), l.data(), l.mesReferencia(), l.metaId(),
                        l.formaPagamento(), List.of(), l.versao()));
            }
        };
        Relogio relogio = new Relogio() {
            @Override
            public Instant agora() {
                return AGORA;
            }

            @Override
            public YearMonth mesAtual() {
                return YearMonth.of(2026, 10);
            }
        };
        gravar = new GravarLancamentos(lancamentos, tags, consulta, identidade, orcamento, relogio);
    }

    static DadosDoLancamento dadosDe(Lancamento l) {
        return new DadosDoLancamento(l.direcao(), l.valor(), l.descricao(), l.data(), l.metaId(), l.formaPagamento(), l.tagIds());
    }

    static DadosDoLancamento gastoNaCasa() {
        return new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("110.00"), "Conta de luz", LocalDate.of(2026, 10, 9), META, FormaPagamento.PIX, Set.of(TAG));
    }

    @Test
    void lancaEDevolveOItemGravado() {
        var item = gravar.lancar(CASA, gastoNaCasa());

        assertThat(item.id().version()).isEqualTo(7);
        assertThat(item.valor()).hasToString("110.00");
        assertThat(item.versao()).isZero();
        assertThat(gravados).containsKey(item.id());
    }

    @Test
    void visualizadorNaoGrava() {
        papelNaCasa = Papel.VIEWER;

        assertThatThrownBy(() -> gravar.lancar(CASA, gastoNaCasa())).isInstanceOf(SemPermissao.class);
        assertThat(gravados).isEmpty();
    }

    @Test
    void espacoDeOutroUsuarioNaoExiste() {
        assertThatThrownBy(() -> gravar.lancar(UUID.randomUUID(), gastoNaCasa())).isInstanceOf(EspacoNaoEncontrado.class);
    }

    @Test
    void metaDeOutroEspacoERecusada() {
        var dados = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("1.00"), "x", LocalDate.of(2026, 10, 9), UUID.randomUUID(), FormaPagamento.PIX, Set.of());

        assertThatThrownBy(() -> gravar.lancar(CASA, dados)).isInstanceOf(RegraDeNegocio.class).hasMessageContaining("Meta");
    }

    @Test
    void tagDeOutroEspacoERecusada() {
        var dados = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("1.00"), "x", LocalDate.of(2026, 10, 9), META, FormaPagamento.PIX, Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> gravar.lancar(CASA, dados)).isInstanceOf(RegraDeNegocio.class).hasMessageContaining("Tag");
    }

    @Test
    void naEmpresaSaidaSemMetaVale() {
        var dados = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("10.00"), "Nota", LocalDate.of(2026, 10, 9), null, FormaPagamento.BOLETO, Set.of());

        assertThat(gravar.lancar(EMPRESA, dados).metaId()).isNull();
    }

    @Test
    void editaComAVersaoCerta() {
        var criado = gravar.lancar(CASA, gastoNaCasa());
        var novo = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("120.00"), "Luz", LocalDate.of(2026, 10, 10), META, FormaPagamento.DEBIT, Set.of());

        var editado = gravar.editar(CASA, criado.id(), novo, criado.versao());

        assertThat(editado.valor()).hasToString("120.00");
        assertThat(editado.versao()).isEqualTo(1);
    }

    @Test
    void editarComVersaoDesatualizadaDaConflito() {
        var criado = gravar.lancar(CASA, gastoNaCasa());
        gravar.editar(CASA, criado.id(), gastoNaCasa(), criado.versao());

        assertThatThrownBy(() -> gravar.editar(CASA, criado.id(), gastoNaCasa(), criado.versao()))
                .isInstanceOf(ConflitoDeVersao.class);
    }

    @Test
    void editarQueNaoExisteDaNaoEncontrado() {
        assertThatThrownBy(() -> gravar.editar(CASA, UUID.randomUUID(), gastoNaCasa(), 0))
                .isInstanceOf(LancamentoNaoEncontrado.class);
    }

    @Test
    void editarAplicaAsRegrasDeNegocio() {
        var criado = gravar.lancar(CASA, gastoNaCasa());
        var invalido = new DadosDoLancamento(
                Direcao.OUT, Dinheiro.deTexto("1.00"), "x", LocalDate.of(2026, 10, 9), null, FormaPagamento.PIX, Set.of());

        assertThatThrownBy(() -> gravar.editar(CASA, criado.id(), invalido, criado.versao())).isInstanceOf(RegraDeNegocio.class);
    }

    @Test
    void excluiEDepoisNaoEncontra() {
        var criado = gravar.lancar(CASA, gastoNaCasa());

        gravar.excluir(CASA, criado.id());

        assertThat(excluidos).containsExactly(criado.id());
        assertThatThrownBy(() -> gravar.excluir(CASA, criado.id())).isInstanceOf(LancamentoNaoEncontrado.class);
    }

    @Test
    void visualizadorNaoExclui() {
        var criado = gravar.lancar(CASA, gastoNaCasa());
        papelNaCasa = Papel.VIEWER;

        assertThatThrownBy(() -> gravar.excluir(CASA, criado.id())).isInstanceOf(SemPermissao.class);
        assertThat(gravados).containsKey(criado.id());
    }

    @Test
    void lancamentoDeOutroEspacoNaoEEditavelPorAqui() {
        var criado = gravar.lancar(CASA, gastoNaCasa());

        assertThatThrownBy(() -> gravar.excluir(EMPRESA, criado.id())).isInstanceOf(LancamentoNaoEncontrado.class);
    }
}
