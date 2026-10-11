package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.application.ConsultaDoMes;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ListaDoMesResponse(
        @NotNull String mes, @NotNull TotaisResponse totais, @NotNull List<LancamentoResponse> itens) {

    static ListaDoMesResponse de(ConsultaDoMes consulta) {
        return new ListaDoMesResponse(
                consulta.mes().toString(),
                TotaisResponse.de(consulta.totais()),
                consulta.itens().stream().map(LancamentoResponse::de).toList());
    }
}
