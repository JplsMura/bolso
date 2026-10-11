package dev.bolso.cartoes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bolso.cartoes.domain.Cartao;
import dev.bolso.cartoes.domain.DadosDoCartao;
import dev.bolso.cartoes.domain.Fatura;
import dev.bolso.cartoes.domain.RegraDoCartao;
import dev.bolso.cartoes.domain.StatusDaFatura;
import dev.bolso.cartoes.domain.VersaoDesatualizada;
import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.EspacoNaoEncontrado;
import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.identidade.Papel;
import dev.bolso.identidade.SemPermissao;
import dev.bolso.identidade.TipoEspaco;
import dev.bolso.lancamentos.ItemDaFatura;
import dev.bolso.lancamentos.LancamentosApi;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Casos de uso de cartões e faturas com portas falsas: nada de Spring nem banco. Valores fictícios. */
class CasosDeUsoDosCartoesTest {

    static final UUID CASA = UUID.fromString("00000000-0000-7000-8000-0000000000c1");
    static final LocalDate HOJE = LocalDate.of(2026, 10, 26);

    Papel papel;
    final Map<UUID, Cartao> cartoes = new LinkedHashMap<>();
    final Map<UUID, Fatura> faturas = new LinkedHashMap<>();
    /** Soma ao vivo dos gastos de cada fatura (o que o Lançamentos devolveria). */
    final Map<UUID, Dinheiro> somas = new HashMap<>();

    GerenciarCartoes gerenciar;
    ConsultarFaturas consultar;
    MudarStatusDaFatura mudar;

    @BeforeEach
    void monta() {
        papel = Papel.OWNER;
        IdentidadeApi identidade = new IdentidadeApi() {
            @Override
            public UUID usuarioAtualId() {
                return UUID.randomUUID();
            }

            @Override
            public Papel papelNoEspaco(UUID espacoId) {
                if (!espacoId.equals(CASA)) {
                    throw new EspacoNaoEncontrado(espacoId);
                }
                return papel;
            }

            @Override
            public TipoEspaco tipoDoEspaco(UUID espacoId) {
                papelNoEspaco(espacoId);
                return TipoEspaco.HOME;
            }

            @Override
            public void exigirPermissaoDeEscrita(UUID espacoId) {
                if (!papelNoEspaco(espacoId).podeEscrever()) {
                    throw new SemPermissao(espacoId, papel);
                }
            }
        };
        CartoesRepository repoCartoes = new CartoesRepository() {
            @Override
            public List<Cartao> listarAtivos(UUID espacoId) {
                return List.copyOf(cartoes.values());
            }

            @Override
            public Optional<Cartao> buscarAtivo(UUID espacoId, UUID id) {
                return Optional.ofNullable(cartoes.get(id));
            }

            @Override
            public Optional<Cartao> buscarMesmoArquivado(UUID espacoId, UUID id) {
                return Optional.ofNullable(cartoes.get(id));
            }

            @Override
            public boolean existeOutroAtivoComNome(UUID espacoId, String nome, UUID excetoId) {
                return cartoes.values().stream().anyMatch(c -> c.nome().equalsIgnoreCase(nome) && !c.id().equals(excetoId));
            }

            @Override
            public void salvar(Cartao c, Instant agora) {
                var versao = cartoes.containsKey(c.id()) ? c.versao() + 1 : 0;
                cartoes.put(c.id(), Cartao.reconstituir(c.id(), c.espacoId(),
                        new DadosDoCartao(c.nome(), c.diaFechamento(), c.diaVencimento(), c.limite()), versao));
            }

            @Override
            public void arquivar(Cartao c, Instant agora) {
                cartoes.remove(c.id());
            }

            @Override
            public Optional<String> nomeDe(UUID espacoId, UUID id) {
                return Optional.ofNullable(cartoes.get(id)).map(Cartao::nome);
            }
        };
        FaturasRepository repoFaturas = new FaturasRepository() {
            @Override
            public Optional<Fatura> buscar(UUID espacoId, UUID id) {
                return Optional.ofNullable(faturas.get(id));
            }

            @Override
            public Optional<Fatura> buscarParaMudar(UUID espacoId, UUID id) {
                return buscar(espacoId, id);
            }

            @Override
            public void salvar(Fatura f, Instant agora) {
                faturas.put(f.id(), Fatura.reconstituir(f.id(), f.espacoId(), f.cartaoId(), f.mes(), f.fechamento(),
                        f.vencimento(), f.status(), f.totalCongelado(), f.pagaEm(), f.versao() + 1));
            }

            @Override
            public Fatura obterOuCriar(Fatura nova) {
                return doCartaoNoMes(nova.espacoId(), nova.cartaoId(), nova.mes()).orElseGet(() -> {
                    faturas.put(nova.id(), nova);
                    return nova;
                });
            }

            @Override
            public Optional<Fatura> doCartaoNoMes(UUID espacoId, UUID cartaoId, YearMonth mes) {
                return faturas.values().stream().filter(f -> f.cartaoId().equals(cartaoId) && f.mes().equals(mes)).findFirst();
            }

            @Override
            public List<Fatura> porIds(UUID espacoId, Collection<UUID> ids, boolean travar) {
                return ids.stream().map(faturas::get).filter(f -> f != null).toList();
            }

            @Override
            public List<Fatura> doMes(UUID espacoId, YearMonth mes) {
                return faturas.values().stream().filter(f -> f.mes().equals(mes)).toList();
            }

            @Override
            public List<Fatura> naoPagas(UUID espacoId) {
                return faturas.values().stream().filter(f -> f.status() != StatusDaFatura.PAID).toList();
            }
        };
        LancamentosApi lancamentos = new LancamentosApi() {
            @Override
            public Map<UUID, Dinheiro> somaPorFatura(UUID espacoId, Collection<UUID> ids) {
                Map<UUID, Dinheiro> mapa = new HashMap<>();
                ids.forEach(id -> mapa.put(id, somas.getOrDefault(id, Dinheiro.ZERO)));
                return mapa;
            }

            @Override
            public List<ItemDaFatura> itensDasFaturas(UUID espacoId, Collection<UUID> ids) {
                return List.of();
            }
        };
        Relogio relogio = new Relogio() {
            @Override
            public Instant agora() {
                return Instant.parse("2026-10-26T12:00:00Z");
            }

            @Override
            public LocalDate hoje() {
                return HOJE;
            }
        };
        var uso = new UsoDoLimite(repoFaturas, lancamentos);
        gerenciar = new GerenciarCartoes(repoCartoes, repoFaturas, uso, identidade, relogio);
        consultar = new ConsultarFaturas(repoCartoes, repoFaturas, uso, lancamentos, identidade, relogio);
        mudar = new MudarStatusDaFatura(repoFaturas, repoCartoes, consultar, lancamentos, identidade, relogio);
    }

    static DadosDoCartao cartaoA() {
        return new DadosDoCartao("Cartão A", 25, 5, Dinheiro.deTexto("4500.00"));
    }

    /** Uma fatura do cartão no mês, com a soma ao vivo informada. */
    Fatura faturaCom(Cartao cartao, YearMonth mes, String soma) {
        var f = Fatura.nova(UUID.randomUUID(), cartao, mes);
        faturas.put(f.id(), f);
        somas.put(f.id(), Dinheiro.deTexto(soma));
        return f;
    }

    @Test
    void limiteUsadoSomaAbertasAoVivoFechadasCongeladasEIgnoraPagas() {
        var cartao = gerenciar.criar(CASA, cartaoA()).cartao();
        var setembro = faturaCom(cartao, YearMonth.of(2026, 9), "300.00");
        var outubro = faturaCom(cartao, YearMonth.of(2026, 10), "1000.00");
        faturaCom(cartao, YearMonth.of(2026, 11), "25.00"); // parcela futura
        mudar.fechar(CASA, setembro.id(), 0);
        mudar.pagar(CASA, setembro.id(), HOJE, 1);
        mudar.fechar(CASA, outubro.id(), 0);
        somas.put(outubro.id(), Dinheiro.deTexto("5000.00")); // não importa mais: o total congelou

        var comUso = gerenciar.listar(CASA).getFirst();

        assertThat(comUso.usado()).hasToString("1025.00");
        assertThat(comUso.disponivel()).hasToString("3475.00");
    }

    @Test
    void disponivelFicaNegativoQuandoPassaDoLimite() {
        var cartao = gerenciar.criar(CASA, cartaoA()).cartao();
        faturaCom(cartao, YearMonth.of(2026, 10), "4600.00");

        assertThat(gerenciar.listar(CASA).getFirst().disponivel()).hasToString("-100.00");
    }

    @Test
    void nomeRepetidoComOutraCaixaERecusado() {
        gerenciar.criar(CASA, cartaoA());

        assertThatThrownBy(() -> gerenciar.criar(CASA, new DadosDoCartao("cartão a", 1, 10, Dinheiro.deTexto("1.00"))))
                .isInstanceOf(RegraDoCartao.class)
                .hasMessageContaining("Já existe");
    }

    @Test
    void editarComVersaoVelhaDaConflito() {
        var id = gerenciar.criar(CASA, cartaoA()).cartao().id();
        gerenciar.editar(CASA, id, cartaoA(), 0);

        assertThatThrownBy(() -> gerenciar.editar(CASA, id, cartaoA(), 0)).isInstanceOf(VersaoDesatualizada.class);
    }

    @Test
    void naoArquivaComFaturaNaoPaga() {
        var cartao = gerenciar.criar(CASA, cartaoA()).cartao();
        faturaCom(cartao, YearMonth.of(2026, 10), "10.00");

        assertThatThrownBy(() -> gerenciar.arquivar(CASA, cartao.id())).hasMessageContaining("fatura não paga");
    }

    @Test
    void arquivaSemFaturaNaoPaga() {
        var cartao = gerenciar.criar(CASA, cartaoA()).cartao();

        gerenciar.arquivar(CASA, cartao.id());

        assertThat(gerenciar.listar(CASA)).isEmpty();
    }

    @Test
    void visualizadorNaoMexe() {
        var cartao = gerenciar.criar(CASA, cartaoA()).cartao();
        var f = faturaCom(cartao, YearMonth.of(2026, 10), "10.00");
        papel = Papel.VIEWER;

        assertThatThrownBy(() -> gerenciar.criar(CASA, cartaoA())).isInstanceOf(SemPermissao.class);
        assertThatThrownBy(() -> mudar.fechar(CASA, f.id(), 0)).isInstanceOf(SemPermissao.class);
        assertThat(gerenciar.listar(CASA)).hasSize(1);
    }

    @Test
    void fecharCongelaASomaDoLancamentoEPagarNaoMudaOTotal() {
        var cartao = gerenciar.criar(CASA, cartaoA()).cartao();
        var f = faturaCom(cartao, YearMonth.of(2026, 10), "1260.00");

        var fechada = mudar.fechar(CASA, f.id(), 0);
        var paga = mudar.pagar(CASA, f.id(), null, 1);

        assertThat(fechada.fatura().status()).isEqualTo(StatusDaFatura.CLOSED);
        assertThat(fechada.total()).hasToString("1260.00");
        assertThat(paga.fatura().pagaEm()).isEqualTo(HOJE); // sem data = hoje
        assertThat(paga.total()).hasToString("1260.00");
    }

    @Test
    void mudarStatusComVersaoVelhaDaConflito() {
        var cartao = gerenciar.criar(CASA, cartaoA()).cartao();
        var f = faturaCom(cartao, YearMonth.of(2026, 10), "10.00");
        mudar.fechar(CASA, f.id(), 0);

        assertThatThrownBy(() -> mudar.reabrir(CASA, f.id(), 0)).isInstanceOf(VersaoDesatualizada.class);
        assertThat(mudar.reabrir(CASA, f.id(), 1).fatura().status()).isEqualTo(StatusDaFatura.OPEN);
    }

    @Test
    void faturasDoMesTrazTodoCartaoAtivoMesmoSemCompra() {
        var a = gerenciar.criar(CASA, cartaoA()).cartao();
        gerenciar.criar(CASA, new DadosDoCartao("Cartão B", 5, 15, Dinheiro.deTexto("8000.00")));
        faturaCom(a, YearMonth.of(2026, 10), "23.90");

        var doMes = consultar.doMes(CASA, YearMonth.of(2026, 10));

        assertThat(doMes.faturas()).hasSize(2);
        assertThat(doMes.faturas().get(0).total()).hasToString("23.90");
        assertThat(doMes.faturas().get(1).fatura()).isNull();
        assertThat(doMes.faturas().get(1).total()).hasToString("0.00");
    }

    @Test
    void faturaDaCompraUsaOCalendarioESabeOStatus() {
        var cartao = gerenciar.criar(CASA, cartaoA()).cartao();
        var f = faturaCom(cartao, YearMonth.of(2026, 10), "10.00");
        mudar.fechar(CASA, f.id(), 0);

        var antes = consultar.faturaDaCompra(CASA, cartao.id(), LocalDate.of(2026, 10, 8));
        var depois = consultar.faturaDaCompra(CASA, cartao.id(), LocalDate.of(2026, 10, 25));

        assertThat(antes.mes()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(antes.status()).isEqualTo(StatusDaFatura.CLOSED);
        assertThat(depois.mes()).isEqualTo(YearMonth.of(2026, 11));
        assertThat(depois.status()).isNull();
        assertThat(depois.vencimento()).isEqualTo(LocalDate.of(2026, 12, 5));
    }
}
