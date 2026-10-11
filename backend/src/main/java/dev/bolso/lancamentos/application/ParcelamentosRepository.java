package dev.bolso.lancamentos.application;

import dev.bolso.lancamentos.domain.Parcelamento;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída: o cabeçalho do parcelamento (as parcelas são lançamentos). */
public interface ParcelamentosRepository {

    /** Parcelamento do espaço que não foi excluído. */
    Optional<Parcelamento> buscar(UUID espacoId, UUID id);

    void criar(Parcelamento parcelamento, Instant agora);

    /**
     * Grava a descrição e sobe a versão, se a versão gravada ainda for a do parcelamento informado.
     *
     * @throws dev.bolso.lancamentos.domain.ConflitoDeVersao se outra operação alterou antes
     */
    void atualizar(Parcelamento parcelamento, Instant agora);

    /** Exclusão lógica (quando não sobra parcela). */
    void excluir(UUID espacoId, UUID id, Instant agora);
}
