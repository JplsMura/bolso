package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.CartaoNaoEncontrado;
import dev.bolso.cartoes.domain.Fatura;
import dev.bolso.compartilhado.UuidV7;
import dev.bolso.lancamentos.FaturaResolvida;
import dev.bolso.lancamentos.FaturasDoCartao;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementa a porta {@link FaturasDoCartao} do Lançamentos. O Lançamentos já conferiu o acesso ao espaço; aqui
 * toda busca filtra por {@code espacoId}.
 */
@Service
@Transactional
class FaturasDoCartaoService implements FaturasDoCartao {

    private final CartoesRepository cartoes;
    private final FaturasRepository faturas;

    FaturasDoCartaoService(CartoesRepository cartoes, FaturasRepository faturas) {
        this.cartoes = cartoes;
        this.faturas = faturas;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<YearMonth> mesDaFaturaDaCompra(UUID espacoId, UUID cartaoId, LocalDate data) {
        return cartoes.buscarAtivo(espacoId, cartaoId).map(c -> c.calendario().mesDaFatura(data));
    }

    @Override
    public FaturaResolvida faturaDoMes(UUID espacoId, UUID cartaoId, YearMonth mes) {
        var cartao = cartoes.buscarAtivo(espacoId, cartaoId).orElseThrow(() -> new CartaoNaoEncontrado(cartaoId));
        var fatura = faturas.obterOuCriar(Fatura.nova(UuidV7.gerar(), cartao, mes));
        return new FaturaResolvida(fatura.id(), cartao.id(), cartao.nome(), fatura.mes(), fatura.aberta());
    }

    @Override
    public Optional<FaturaResolvida> faturaTravada(UUID espacoId, UUID faturaId) {
        return resolver(espacoId, faturas.porIds(espacoId, List.of(faturaId), true)).values().stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, FaturaResolvida> faturas(UUID espacoId, Collection<UUID> faturaIds) {
        return faturaIds.isEmpty() ? Map.of() : resolver(espacoId, faturas.porIds(espacoId, faturaIds, false));
    }

    /** Inclui cartões arquivados: um gasto antigo continua mostrando o mês da fatura. */
    private Map<UUID, FaturaResolvida> resolver(UUID espacoId, List<Fatura> lista) {
        Map<UUID, String> nomes = new HashMap<>();
        for (var f : lista) {
            nomes.computeIfAbsent(f.cartaoId(), id -> cartoes.nomeDe(espacoId, id).orElse("cartão"));
        }
        return lista.stream()
                .map(f -> new FaturaResolvida(f.id(), f.cartaoId(), nomes.get(f.cartaoId()), f.mes(), f.aberta()))
                .collect(Collectors.toMap(FaturaResolvida::id, Function.identity()));
    }
}
