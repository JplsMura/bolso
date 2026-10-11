package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.Cartao;
import dev.bolso.cartoes.domain.CartaoNaoEncontrado;
import dev.bolso.cartoes.domain.Fatura;
import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.lancamentos.ItemDaFatura;
import dev.bolso.lancamentos.LancamentosApi;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Faturas do mês (por cartão ativo, com itens e total) e a previsão de fatura para uma compra. */
@Service
@Transactional(readOnly = true)
public class ConsultarFaturas {

    private final CartoesRepository cartoes;
    private final FaturasRepository faturas;
    private final UsoDoLimite uso;
    private final LancamentosApi lancamentos;
    private final IdentidadeApi identidade;
    private final Relogio relogio;

    public ConsultarFaturas(
            CartoesRepository cartoes,
            FaturasRepository faturas,
            UsoDoLimite uso,
            LancamentosApi lancamentos,
            IdentidadeApi identidade,
            Relogio relogio) {
        this.cartoes = cartoes;
        this.faturas = faturas;
        this.uso = uso;
        this.lancamentos = lancamentos;
        this.identidade = identidade;
        this.relogio = relogio;
    }

    /** {@code mes} nulo = mês atual. Um item por cartão ativo, com ou sem fatura no mês. */
    public FaturasDoMes doMes(UUID espacoId, YearMonth mes) {
        identidade.papelNoEspaco(espacoId); // 404 se não for membro
        var efetivo = mes != null ? mes : relogio.mesAtual();
        var comUso = uso.de(espacoId, cartoes.listarAtivos(espacoId));
        Map<UUID, Fatura> porCartao = faturas.doMes(espacoId, efetivo).stream()
                .collect(Collectors.toMap(Fatura::cartaoId, Function.identity()));
        var lista = montar(espacoId, comUso, porCartao);
        return new FaturasDoMes(efetivo, lista);
    }

    /** Um cartão e a fatura informada, como a tela mostra depois de mudar o status. */
    public FaturaDoCartao daFatura(UUID espacoId, Cartao cartao, Fatura fatura) {
        return montar(espacoId, List.of(uso.de(espacoId, cartao)), Map.of(cartao.id(), fatura)).getFirst();
    }

    public PrevisaoDaFatura faturaDaCompra(UUID espacoId, UUID cartaoId, LocalDate data) {
        identidade.papelNoEspaco(espacoId);
        var cartao = cartoes.buscarAtivo(espacoId, cartaoId).orElseThrow(() -> new CartaoNaoEncontrado(cartaoId));
        var calendario = cartao.calendario();
        var mes = calendario.mesDaFatura(data);
        var existente = faturas.doCartaoNoMes(espacoId, cartaoId, mes);
        return new PrevisaoDaFatura(
                mes,
                existente.map(Fatura::fechamento).orElse(calendario.fechamento(mes)),
                existente.map(Fatura::vencimento).orElse(calendario.vencimento(mes)),
                existente.map(Fatura::status).orElse(null));
    }

    private List<FaturaDoCartao> montar(UUID espacoId, List<CartaoComUso> cartoesComUso, Map<UUID, Fatura> porCartao) {
        var ids = porCartao.values().stream().map(Fatura::id).toList();
        var somas = lancamentos.somaPorFatura(espacoId, ids);
        Map<UUID, List<ItemDaFatura>> itens = lancamentos.itensDasFaturas(espacoId, ids).stream()
                .collect(Collectors.groupingBy(ItemDaFatura::faturaId));
        return cartoesComUso.stream()
                .map(c -> {
                    var fatura = porCartao.get(c.cartao().id());
                    if (fatura == null) {
                        return new FaturaDoCartao(c, null, Dinheiro.ZERO, List.of());
                    }
                    var total = fatura.total(somas.getOrDefault(fatura.id(), Dinheiro.ZERO));
                    return new FaturaDoCartao(c, fatura, total, itens.getOrDefault(fatura.id(), List.of()));
                })
                .toList();
    }
}
