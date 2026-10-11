package dev.bolso.cartoes.domain;

import dev.bolso.compartilhado.Dinheiro;
import java.util.UUID;

/**
 * Cartão de crédito de um espaço: nome (único entre os ativos do espaço, conferido pelo caso de uso), dias de
 * fechamento e vencimento, e limite. Mudar o dia de fechamento vale para as faturas que ainda não existem; as que
 * existem guardam as suas datas.
 */
public final class Cartao {

    public static final int MAXIMO_NOME = 40;

    private final UUID id;
    private final UUID espacoId;
    private final String nome;
    private final CalendarioDoCartao calendario;
    private final Dinheiro limite;
    private final long versao;

    private Cartao(UUID id, UUID espacoId, String nome, CalendarioDoCartao calendario, Dinheiro limite, long versao) {
        this.id = id;
        this.espacoId = espacoId;
        this.nome = nome;
        this.calendario = calendario;
        this.limite = limite;
        this.versao = versao;
    }

    public static Cartao novo(UUID id, UUID espacoId, DadosDoCartao dados) {
        return validar(id, espacoId, dados, 0);
    }

    public Cartao editar(DadosDoCartao dados) {
        return validar(id, espacoId, dados, versao);
    }

    public static Cartao reconstituir(UUID id, UUID espacoId, DadosDoCartao dados, long versao) {
        return new Cartao(id, espacoId, dados.nome(), new CalendarioDoCartao(dados.diaFechamento(), dados.diaVencimento()),
                dados.limite(), versao);
    }

    private static Cartao validar(UUID id, UUID espacoId, DadosDoCartao d, long versao) {
        var nome = d.nome() == null ? "" : d.nome().trim();
        if (nome.isEmpty() || nome.length() > MAXIMO_NOME) {
            throw new RegraDoCartao("O nome do cartão deve ter de 1 a " + MAXIMO_NOME + " caracteres");
        }
        if (d.limite() == null || !d.limite().ehPositivo()) {
            throw new RegraDoCartao("O limite deve ser maior que zero");
        }
        return new Cartao(id, espacoId, nome, new CalendarioDoCartao(d.diaFechamento(), d.diaVencimento()), d.limite(), versao);
    }

    public UUID id() {
        return id;
    }

    public UUID espacoId() {
        return espacoId;
    }

    public String nome() {
        return nome;
    }

    public CalendarioDoCartao calendario() {
        return calendario;
    }

    public int diaFechamento() {
        return calendario.diaFechamento();
    }

    public int diaVencimento() {
        return calendario.diaVencimento();
    }

    public Dinheiro limite() {
        return limite;
    }

    public long versao() {
        return versao;
    }
}
