package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.Cartao;
import dev.bolso.cartoes.domain.Fatura;
import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.lancamentos.LancamentosApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Limite usado de cada cartão: soma das faturas não pagas. Aberta conta pela soma ao vivo dos gastos (vinda do
 * Lançamentos); fechada, pelo total congelado.
 */
@Component
public class UsoDoLimite {

    private final FaturasRepository faturas;
    private final LancamentosApi lancamentos;

    public UsoDoLimite(FaturasRepository faturas, LancamentosApi lancamentos) {
        this.faturas = faturas;
        this.lancamentos = lancamentos;
    }

    public List<CartaoComUso> de(UUID espacoId, List<Cartao> cartoes) {
        var naoPagas = faturas.naoPagas(espacoId);
        var abertas = naoPagas.stream().filter(Fatura::aberta).map(Fatura::id).toList();
        var somas = lancamentos.somaPorFatura(espacoId, abertas);
        Map<UUID, Dinheiro> usadoPorCartao = new HashMap<>();
        for (var f : naoPagas) {
            var total = f.total(somas.getOrDefault(f.id(), Dinheiro.ZERO));
            usadoPorCartao.merge(f.cartaoId(), total, Dinheiro::somar);
        }
        return cartoes.stream()
                .map(c -> new CartaoComUso(c, usadoPorCartao.getOrDefault(c.id(), Dinheiro.ZERO)))
                .toList();
    }

    public CartaoComUso de(UUID espacoId, Cartao cartao) {
        return de(espacoId, List.of(cartao)).getFirst();
    }
}
