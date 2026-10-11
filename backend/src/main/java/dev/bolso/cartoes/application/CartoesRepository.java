package dev.bolso.cartoes.application;

import dev.bolso.cartoes.domain.Cartao;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída: cartões do espaço. "Ativo" = não arquivado. */
public interface CartoesRepository {

    /** Ativos, por nome. */
    List<Cartao> listarAtivos(UUID espacoId);

    Optional<Cartao> buscarAtivo(UUID espacoId, UUID id);

    /** O cartão, mesmo arquivado (faturas antigas continuam mudando de status). */
    Optional<Cartao> buscarMesmoArquivado(UUID espacoId, UUID id);

    /** Existe outro cartão ativo com este nome (sem diferenciar maiúsculas)? {@code excetoId} pode ser nulo. */
    boolean existeOutroAtivoComNome(UUID espacoId, String nome, UUID excetoId);

    /** Grava (novo ou existente). Lança {@code VersaoDesatualizada} se outra operação alterou antes. */
    void salvar(Cartao cartao, Instant agora);

    void arquivar(Cartao cartao, Instant agora);

    /** Nome do cartão, mesmo arquivado (para mensagens sobre faturas antigas). */
    Optional<String> nomeDe(UUID espacoId, UUID id);
}
