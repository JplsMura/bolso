package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.CartaoNaoEncontrado;
import dev.bolso.cartoes.domain.Fatura;
import dev.bolso.cartoes.domain.FaturaNaoEncontrada;
import dev.bolso.cartoes.domain.VersaoDesatualizada;
import dev.bolso.compartilhado.Dinheiro;
import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.lancamentos.LancamentosApi;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fechar, pagar, reabrir e desfazer o pagamento. A fatura é travada para escrita antes de somar os gastos, para o
 * total congelado não perder uma compra que esteja entrando ao mesmo tempo.
 */
@Service
@Transactional
public class MudarStatusDaFatura {

    private final FaturasRepository faturas;
    private final CartoesRepository cartoes;
    private final ConsultarFaturas consultar;
    private final LancamentosApi lancamentos;
    private final IdentidadeApi identidade;
    private final Relogio relogio;

    public MudarStatusDaFatura(
            FaturasRepository faturas,
            CartoesRepository cartoes,
            ConsultarFaturas consultar,
            LancamentosApi lancamentos,
            IdentidadeApi identidade,
            Relogio relogio) {
        this.faturas = faturas;
        this.cartoes = cartoes;
        this.consultar = consultar;
        this.lancamentos = lancamentos;
        this.identidade = identidade;
        this.relogio = relogio;
    }

    public FaturaDoCartao fechar(UUID espacoId, UUID id, long versaoLida) {
        return mudar(espacoId, id, versaoLida, f -> {
            var soma = lancamentos.somaPorFatura(espacoId, List.of(f.id())).getOrDefault(f.id(), Dinheiro.ZERO);
            return f.fechar(relogio.hoje(), soma);
        });
    }

    /** {@code pagaEm} nulo = hoje. */
    public FaturaDoCartao pagar(UUID espacoId, UUID id, LocalDate pagaEm, long versaoLida) {
        var hoje = relogio.hoje();
        return mudar(espacoId, id, versaoLida, f -> f.pagar(pagaEm != null ? pagaEm : hoje, hoje));
    }

    public FaturaDoCartao reabrir(UUID espacoId, UUID id, long versaoLida) {
        return mudar(espacoId, id, versaoLida, Fatura::reabrir);
    }

    public FaturaDoCartao desfazerPagamento(UUID espacoId, UUID id, long versaoLida) {
        return mudar(espacoId, id, versaoLida, Fatura::desfazerPagamento);
    }

    private FaturaDoCartao mudar(UUID espacoId, UUID id, long versaoLida, UnaryOperator<Fatura> mudanca) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var atual = faturas.buscarParaMudar(espacoId, id).orElseThrow(() -> new FaturaNaoEncontrada(id));
        if (atual.versao() != versaoLida) {
            throw new VersaoDesatualizada(id);
        }
        var nova = mudanca.apply(atual);
        faturas.salvar(nova, relogio.agora());
        var cartao = cartoes.buscarMesmoArquivado(espacoId, nova.cartaoId())
                .orElseThrow(() -> new CartaoNaoEncontrado(nova.cartaoId()));
        return consultar.daFatura(espacoId, cartao, faturas.buscar(espacoId, id).orElseThrow());
    }
}
