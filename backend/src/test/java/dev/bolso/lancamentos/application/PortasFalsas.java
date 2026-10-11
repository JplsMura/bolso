package dev.bolso.lancamentos.application;

import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.EspacoNaoEncontrado;
import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.identidade.Papel;
import dev.bolso.identidade.SemPermissao;
import dev.bolso.identidade.TipoEspaco;
import dev.bolso.lancamentos.FaturaResolvida;
import dev.bolso.lancamentos.FaturasDoCartao;
import dev.bolso.lancamentos.ItemDaFatura;
import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.DadosDoLancamento;
import dev.bolso.lancamentos.domain.Lancamento;
import dev.bolso.lancamentos.domain.Parcelamento;
import dev.bolso.lancamentos.domain.Tag;
import dev.bolso.orcamento.OrcamentoApi;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Portas falsas, em memória, para os casos de uso de escrita: nada de Spring nem banco. O cartão falso fecha no
 * dia 25; a fatura é identificada por (cartão, mês) e pode ser marcada como fechada.
 */
class PortasFalsas {

    static final UUID CASA = UUID.fromString("00000000-0000-7000-8000-0000000000c1");
    static final UUID EMPRESA = UUID.fromString("00000000-0000-7000-8000-0000000000c2");
    static final UUID META = UUID.fromString("00000000-0000-7000-8000-0000000000a1");
    static final UUID TAG = UUID.fromString("00000000-0000-7000-8000-0000000000b1");
    static final UUID CARTAO = UUID.fromString("00000000-0000-7000-8000-0000000000d1");
    static final UUID CARTAO_DA_EMPRESA = UUID.fromString("00000000-0000-7000-8000-0000000000d2");
    static final Instant AGORA = Instant.parse("2026-10-09T12:00:00Z");
    static final int DIA_FECHAMENTO = 25;

    Papel papel = Papel.OWNER;
    final Map<UUID, Lancamento> gravados = new LinkedHashMap<>();
    final List<UUID> excluidos = new ArrayList<>();
    final Map<UUID, Parcelamento> parcelamentosGravados = new HashMap<>();
    final Set<UUID> parcelamentosExcluidos = new HashSet<>();
    /** id da fatura por "cartão|mês". */
    final Map<String, UUID> faturasCriadas = new HashMap<>();
    final Map<UUID, YearMonth> mesDaFatura = new HashMap<>();
    final Map<UUID, UUID> cartaoDaFatura = new HashMap<>();
    final Set<String> fechadas = new HashSet<>();

    final IdentidadeApi identidade = new IdentidadeApi() {
        @Override
        public UUID usuarioAtualId() {
            return UUID.randomUUID();
        }

        @Override
        public Papel papelNoEspaco(UUID espacoId) {
            if (!espacoId.equals(CASA) && !espacoId.equals(EMPRESA)) {
                throw new EspacoNaoEncontrado(espacoId);
            }
            return papel;
        }

        @Override
        public TipoEspaco tipoDoEspaco(UUID espacoId) {
            papelNoEspaco(espacoId);
            return espacoId.equals(CASA) ? TipoEspaco.HOME : TipoEspaco.COMPANY;
        }

        @Override
        public void exigirPermissaoDeEscrita(UUID espacoId) {
            var p = papelNoEspaco(espacoId);
            if (!p.podeEscrever()) {
                throw new SemPermissao(espacoId, p);
            }
        }
    };

    final OrcamentoApi orcamento = (espacoId, metaId) -> espacoId.equals(CASA) && META.equals(metaId);

    final TagsRepository tags = new TagsRepository() {
        @Override
        public List<Tag> listarAtivas(UUID espacoId) {
            return List.of();
        }

        @Override
        public boolean existeNome(UUID espacoId, String nome) {
            return false;
        }

        @Override
        public void salvar(Tag tag) {}

        @Override
        public boolean todasAtivasNoEspaco(UUID espacoId, Set<UUID> tagIds) {
            return espacoId.equals(CASA) ? Set.of(TAG).containsAll(tagIds) : tagIds.isEmpty();
        }
    };

    final LancamentosRepository lancamentos = new LancamentosRepository() {
        @Override
        public Optional<Lancamento> buscar(UUID espacoId, UUID id) {
            return Optional.ofNullable(gravados.get(id)).filter(l -> l.espacoId().equals(espacoId));
        }

        @Override
        public List<Lancamento> parcelasDe(UUID espacoId, UUID parcelamentoId) {
            return gravados.values().stream()
                    .filter(l -> l.espacoId().equals(espacoId) && l.ehParcela() && l.parcela().parcelamentoId().equals(parcelamentoId))
                    .sorted(Comparator.comparingInt(l -> l.parcela().numero()))
                    .toList();
        }

        @Override
        public void salvar(Lancamento l, Instant agora) {
            // como o JPA: a versão sobe a cada gravação de algo que já existia
            var versao = gravados.containsKey(l.id()) ? gravados.get(l.id()).versao() + 1 : 0;
            gravados.put(l.id(), Lancamento.reconstituir(l.id(), l.espacoId(), l.dados(), l.faturaId(), l.parcela(), versao));
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

    final ParcelamentosRepository parcelamentos = new ParcelamentosRepository() {
        @Override
        public Optional<Parcelamento> buscar(UUID espacoId, UUID id) {
            return Optional.ofNullable(parcelamentosGravados.get(id))
                    .filter(p -> p.espacoId().equals(espacoId) && !parcelamentosExcluidos.contains(id));
        }

        @Override
        public void criar(Parcelamento p, Instant agora) {
            parcelamentosGravados.put(p.id(), p);
        }

        @Override
        public void atualizar(Parcelamento p, Instant agora) {
            var gravado = parcelamentosGravados.get(p.id());
            if (gravado.versao() != p.versao()) {
                throw new ConflitoDeVersao(p.id());
            }
            parcelamentosGravados.put(p.id(), Parcelamento.reconstituir(
                    p.id(), p.espacoId(), p.cartaoId(), p.descricao(), p.valorTotal(), p.parcelas(), p.parcelaAtual(),
                    p.dataDaCompra(), p.versao() + 1));
        }

        @Override
        public void excluir(UUID espacoId, UUID id, Instant agora) {
            parcelamentosExcluidos.add(id);
        }
    };

    final FaturasDoCartao faturas = new FaturasDoCartao() {
        @Override
        public Optional<YearMonth> mesDaFaturaDaCompra(UUID espacoId, UUID cartaoId, LocalDate data) {
            var doEspaco = (espacoId.equals(CASA) && CARTAO.equals(cartaoId))
                    || (espacoId.equals(EMPRESA) && CARTAO_DA_EMPRESA.equals(cartaoId));
            if (!doEspaco) {
                return Optional.empty();
            }
            var mes = YearMonth.from(data);
            return Optional.of(data.getDayOfMonth() < DIA_FECHAMENTO ? mes : mes.plusMonths(1));
        }

        @Override
        public FaturaResolvida faturaDoMes(UUID espacoId, UUID cartaoId, YearMonth mes) {
            var id = faturasCriadas.computeIfAbsent(chave(cartaoId, mes), k -> UUID.randomUUID());
            mesDaFatura.put(id, mes);
            cartaoDaFatura.put(id, cartaoId);
            return resolvida(id);
        }

        @Override
        public Optional<FaturaResolvida> faturaTravada(UUID espacoId, UUID faturaId) {
            return Optional.ofNullable(mesDaFatura.containsKey(faturaId) ? resolvida(faturaId) : null);
        }

        @Override
        public Map<UUID, FaturaResolvida> faturas(UUID espacoId, Collection<UUID> faturaIds) {
            Map<UUID, FaturaResolvida> mapa = new HashMap<>();
            faturaIds.stream().filter(mesDaFatura::containsKey).forEach(id -> mapa.put(id, resolvida(id)));
            return mapa;
        }
    };

    /** Lê o que foi gravado como a consulta SQL leria (sem tags nem mês da fatura). */
    final ConsultaDeLancamentos consulta = new ConsultaDeLancamentos() {
        @Override
        public ConsultaDoMes consultarMes(UUID espacoId, YearMonth mes, FiltrosDeLancamento filtros) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ItemDeLancamento> buscar(UUID espacoId, UUID id) {
            return lancamentos.buscar(espacoId, id).map(PortasFalsas::item);
        }

        @Override
        public List<ItemDeLancamento> parcelasDoParcelamento(UUID espacoId, UUID parcelamentoId) {
            return lancamentos.parcelasDe(espacoId, parcelamentoId).stream().map(PortasFalsas::item).toList();
        }

        @Override
        public Map<UUID, Dinheiro> somaPorFatura(UUID espacoId, Collection<UUID> faturaIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ItemDaFatura> itensDasFaturas(UUID espacoId, Collection<UUID> faturaIds) {
            throw new UnsupportedOperationException();
        }
    };

    final Relogio relogio = new Relogio() {
        @Override
        public Instant agora() {
            return AGORA;
        }

        @Override
        public YearMonth mesAtual() {
            return YearMonth.of(2026, 10);
        }
    };

    final ConferenciasDoLancamento conferencias = new ConferenciasDoLancamento(tags, orcamento, faturas);
    final ConsultarLancamentos consultar = new ConsultarLancamentos(consulta, faturas, identidade, relogio);

    GravarLancamentos gravarLancamentos() {
        return new GravarLancamentos(lancamentos, consultar, conferencias, identidade, relogio);
    }

    GravarParcelamentos gravarParcelamentos() {
        var consultarParcelamentos = new ConsultarParcelamentos(parcelamentos, consulta, consultar, faturas, identidade);
        return new GravarParcelamentos(parcelamentos, lancamentos, consultarParcelamentos, conferencias, identidade, relogio);
    }

    /** Fecha a fatura do cartão naquele mês (criando, se ainda não existe). */
    void fechar(UUID cartaoId, YearMonth mes) {
        faturas.faturaDoMes(CASA, cartaoId, mes);
        fechadas.add(chave(cartaoId, mes));
    }

    YearMonth mesDa(UUID faturaId) {
        return mesDaFatura.get(faturaId);
    }

    private FaturaResolvida resolvida(UUID id) {
        var cartao = cartaoDaFatura.get(id);
        var mes = mesDaFatura.get(id);
        return new FaturaResolvida(id, cartao, "Cartão de teste", mes, !fechadas.contains(chave(cartao, mes)));
    }

    private static String chave(UUID cartaoId, YearMonth mes) {
        return cartaoId + "|" + mes;
    }

    static ItemDeLancamento item(Lancamento l) {
        return new ItemDeLancamento(
                l.id(), l.direcao(), l.valor(), l.descricao(), l.data(), l.mesReferencia(), l.metaId(), l.formaPagamento(),
                l.cartaoId(), l.faturaId(), null, l.parcela(), List.of(), l.versao());
    }

    static DadosDoLancamento comDados(Lancamento l) {
        return l.dados();
    }
}
