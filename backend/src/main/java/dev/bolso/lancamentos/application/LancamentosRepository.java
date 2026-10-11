package dev.bolso.lancamentos.application;

import dev.bolso.lancamentos.domain.Lancamento;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída: gravação dos lançamentos. */
public interface LancamentosRepository {

    /** Lançamento do espaço que não foi excluído. */
    Optional<Lancamento> buscar(UUID espacoId, UUID id);

    /** Parcelas não excluídas de um parcelamento, pela ordem do número. */
    List<Lancamento> parcelasDe(UUID espacoId, UUID parcelamentoId);

    /** Grava (novo ou existente). Lança {@code ConflitoDeVersao} se outra operação alterou antes. */
    void salvar(Lancamento lancamento, Instant agora);

    /** Exclusão lógica. Devolve false se não existe (ou já foi excluído). */
    boolean excluir(UUID espacoId, UUID id, Instant agora);
}
