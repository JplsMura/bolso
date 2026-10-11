package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.Cartao;
import dev.bolso.cartoes.domain.CartaoNaoEncontrado;
import dev.bolso.cartoes.domain.DadosDoCartao;
import dev.bolso.cartoes.domain.RegraDoCartao;
import dev.bolso.cartoes.domain.VersaoDesatualizada;
import dev.bolso.compartilhado.UuidV7;
import dev.bolso.identidade.IdentidadeApi;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de cartões: listar com o limite usado, criar, editar e arquivar. */
@Service
@Transactional
public class GerenciarCartoes {

    private final CartoesRepository cartoes;
    private final FaturasRepository faturas;
    private final UsoDoLimite uso;
    private final IdentidadeApi identidade;
    private final Relogio relogio;

    public GerenciarCartoes(
            CartoesRepository cartoes, FaturasRepository faturas, UsoDoLimite uso, IdentidadeApi identidade, Relogio relogio) {
        this.cartoes = cartoes;
        this.faturas = faturas;
        this.uso = uso;
        this.identidade = identidade;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<CartaoComUso> listar(UUID espacoId) {
        identidade.papelNoEspaco(espacoId); // 404 se não for membro
        return uso.de(espacoId, cartoes.listarAtivos(espacoId));
    }

    public CartaoComUso criar(UUID espacoId, DadosDoCartao dados) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var cartao = Cartao.novo(UuidV7.gerar(), espacoId, dados);
        exigirNomeLivre(cartao);
        cartoes.salvar(cartao, relogio.agora());
        return uso.de(espacoId, buscar(espacoId, cartao.id()));
    }

    public CartaoComUso editar(UUID espacoId, UUID id, DadosDoCartao dados, long versaoLida) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var atual = buscar(espacoId, id);
        if (atual.versao() != versaoLida) {
            throw new VersaoDesatualizada(id);
        }
        var editado = atual.editar(dados);
        exigirNomeLivre(editado);
        cartoes.salvar(editado, relogio.agora());
        return uso.de(espacoId, buscar(espacoId, id));
    }

    /** Arquivar some com o cartão das listas e do formulário; os gastos e as faturas ficam. */
    public void arquivar(UUID espacoId, UUID id) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var cartao = buscar(espacoId, id);
        if (faturas.naoPagas(espacoId).stream().anyMatch(f -> f.cartaoId().equals(id))) {
            throw new RegraDoCartao("O " + cartao.nome() + " tem fatura não paga. Pague as faturas antes de arquivar.");
        }
        cartoes.arquivar(cartao, relogio.agora());
    }

    private Cartao buscar(UUID espacoId, UUID id) {
        return cartoes.buscarAtivo(espacoId, id).orElseThrow(() -> new CartaoNaoEncontrado(id));
    }

    private void exigirNomeLivre(Cartao cartao) {
        if (cartoes.existeOutroAtivoComNome(cartao.espacoId(), cartao.nome(), cartao.id())) {
            throw new RegraDoCartao("Já existe um cartão com o nome " + cartao.nome());
        }
    }
}
