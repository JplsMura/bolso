package dev.bolso.lancamentos.application;

import dev.bolso.lancamentos.domain.Tag;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Porta de saída: tags do espaço. */
public interface TagsRepository {

    /** Tags não arquivadas, por nome. */
    List<Tag> listarAtivas(UUID espacoId);

    /** Já existe uma tag com este nome (sem diferenciar maiúsculas)? */
    boolean existeNome(UUID espacoId, String nome);

    void salvar(Tag tag);

    /** Todas as tags existem, estão ativas e são deste espaço? Conjunto vazio vale. */
    boolean todasAtivasNoEspaco(UUID espacoId, Set<UUID> tagIds);
}
