package fixtures.arquitetura.domain;

import org.springframework.context.ApplicationEventPublisher;

/** Violação proposital (só para ArquiteturaHexagonalMordeTest): domínio usando Spring. */
public class DominioComSpring {
    ApplicationEventPublisher eventos;
}
