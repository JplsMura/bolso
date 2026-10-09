package fixtures.arquitetura.domain;

import jakarta.persistence.EntityManager;

/** Violação proposital (só para ArquiteturaHexagonalMordeTest): domínio usando JPA. */
public class DominioComJpa {
    EntityManager gerenciador;
}
