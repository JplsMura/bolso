package dev.bolso.lancamentos.application;

import dev.bolso.compartilhado.UuidV7;
import dev.bolso.identidade.IdentidadeApi;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import dev.bolso.lancamentos.domain.Tag;
import dev.bolso.lancamentos.domain.TipoCusto;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GerenciarTags {

    private final TagsRepository tags;
    private final IdentidadeApi identidade;

    public GerenciarTags(TagsRepository tags, IdentidadeApi identidade) {
        this.tags = tags;
        this.identidade = identidade;
    }

    @Transactional(readOnly = true)
    public List<Tag> listar(UUID espacoId) {
        identidade.papelNoEspaco(espacoId); // 404 se não for membro
        return tags.listarAtivas(espacoId);
    }

    public Tag criar(UUID espacoId, String nome, TipoCusto tipoCusto) {
        identidade.exigirPermissaoDeEscrita(espacoId);
        var tag = Tag.nova(UuidV7.gerar(), espacoId, nome, tipoCusto);
        if (tags.existeNome(espacoId, tag.nome())) {
            throw new RegraDeNegocio("Já existe uma tag com o nome " + tag.nome());
        }
        tags.salvar(tag);
        return tag;
    }
}
