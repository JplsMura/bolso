package dev.bolso.lancamentos.application;

import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.lancamentos.domain.LancamentoNaoEncontrado;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leituras: lista do mês com filtros e busca, e um lançamento. */
@Service
@Transactional(readOnly = true)
public class ConsultarLancamentos {

    private final ConsultaDeLancamentos consulta;
    private final IdentidadeApi identidade;
    private final Relogio relogio;

    public ConsultarLancamentos(ConsultaDeLancamentos consulta, IdentidadeApi identidade, Relogio relogio) {
        this.consulta = consulta;
        this.identidade = identidade;
        this.relogio = relogio;
    }

    /** {@code mes} nulo = mês atual. Texto em branco não filtra. */
    public ConsultaDoMes consultarMes(UUID espacoId, YearMonth mes, FiltrosDeLancamento filtros) {
        identidade.papelNoEspaco(espacoId); // 404 se não for membro
        var texto = filtros.texto() == null || filtros.texto().isBlank() ? null : filtros.texto().trim();
        var efetivos = new FiltrosDeLancamento(texto, filtros.metaId(), filtros.tagId(), filtros.formaPagamento(), filtros.direcao());
        return consulta.consultarMes(espacoId, mes != null ? mes : relogio.mesAtual(), efetivos);
    }

    public ItemDeLancamento buscar(UUID espacoId, UUID id) {
        identidade.papelNoEspaco(espacoId);
        return consulta.buscar(espacoId, id).orElseThrow(() -> new LancamentoNaoEncontrado(id));
    }
}
