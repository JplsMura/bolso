package dev.bolso.lancamentos.application;

import dev.bolso.lancamentos.domain.Parcelamento;
import java.util.List;
import java.util.UUID;

/**
 * O parcelamento e as parcelas que sobram (não excluídas). Meta e tags vêm da última parcela, que sempre recebe as
 * edições (edição só vale para faturas abertas, e as abertas são as mais novas).
 */
public record VisaoDoParcelamento(
        Parcelamento parcelamento, UUID metaId, List<TagResumo> tags, List<ParcelaDoParcelamento> parcelas) {}
