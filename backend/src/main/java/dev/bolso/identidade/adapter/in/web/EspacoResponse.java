package dev.bolso.identidade.adapter.in.web;

import dev.bolso.identidade.Papel;
import dev.bolso.identidade.domain.Espaco;
import dev.bolso.identidade.domain.TipoEspaco;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Contrato da API: campos em português, valores dos enums em inglês. {@code @NotNull} só existe para
 * o OpenAPI marcar os campos como obrigatórios (o tipo gerado no front fica sem {@code ?}).
 */
public record EspacoResponse(
        @NotNull UUID id, @NotNull TipoEspaco tipo, @NotNull String nome, @NotNull Papel papel) {

    static EspacoResponse de(Espaco espaco) {
        return new EspacoResponse(espaco.id(), espaco.tipo(), espaco.nome(), espaco.papel());
    }
}
