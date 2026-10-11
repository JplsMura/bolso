package dev.bolso.lancamentos.application;

import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.lancamentos.FaturaResolvida;
import dev.bolso.lancamentos.FaturasDoCartao;
import dev.bolso.lancamentos.domain.ParcelamentoNaoEncontrado;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsultarParcelamentos {

    private final ParcelamentosRepository parcelamentos;
    private final ConsultaDeLancamentos consulta;
    private final ConsultarLancamentos lancamentos;
    private final FaturasDoCartao faturas;
    private final IdentidadeApi identidade;

    public ConsultarParcelamentos(
            ParcelamentosRepository parcelamentos,
            ConsultaDeLancamentos consulta,
            ConsultarLancamentos lancamentos,
            FaturasDoCartao faturas,
            IdentidadeApi identidade) {
        this.parcelamentos = parcelamentos;
        this.consulta = consulta;
        this.lancamentos = lancamentos;
        this.faturas = faturas;
        this.identidade = identidade;
    }

    public VisaoDoParcelamento buscar(UUID espacoId, UUID id) {
        identidade.papelNoEspaco(espacoId); // 404 se não for membro
        var parcelamento = parcelamentos.buscar(espacoId, id).orElseThrow(() -> new ParcelamentoNaoEncontrado(id));
        var itens = lancamentos.comMesDaFatura(espacoId, consulta.parcelasDoParcelamento(espacoId, id));
        var ids = itens.stream().map(ItemDeLancamento::faturaId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, FaturaResolvida> porId = ids.isEmpty() ? Map.of() : faturas.faturas(espacoId, ids);
        List<ParcelaDoParcelamento> parcelas = itens.stream()
                .map(i -> new ParcelaDoParcelamento(i, porId.containsKey(i.faturaId()) && porId.get(i.faturaId()).aberta()))
                .toList();
        var ultima = itens.isEmpty() ? null : itens.getLast();
        return new VisaoDoParcelamento(
                parcelamento, ultima == null ? null : ultima.metaId(), ultima == null ? List.of() : ultima.tags(), parcelas);
    }
}
