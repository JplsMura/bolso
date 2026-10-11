package dev.bolso.lancamentos;

import dev.bolso.compartilhado.Dinheiro;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um gasto como a fatura o mostra. {@code numeroDaParcela} e {@code totalDeParcelas} são nulos fora de
 * parcelamento; {@code metaId} é nulo na Empresa.
 */
public record ItemDaFatura(
        UUID faturaId,
        UUID lancamentoId,
        String descricao,
        LocalDate data,
        UUID metaId,
        Dinheiro valor,
        Integer numeroDaParcela,
        Integer totalDeParcelas) {}
