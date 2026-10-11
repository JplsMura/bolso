package dev.bolso.cartoes.adapter.out.relogio;

import dev.bolso.cartoes.application.Relogio;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** O nome não é {@code RelogioSistema} para não colidir com o bean do Lançamentos. */
@Component
class RelogioDosCartoes implements Relogio {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final Clock relogio = Clock.systemUTC();

    @Override
    public Instant agora() {
        return relogio.instant();
    }

    @Override
    public LocalDate hoje() {
        return LocalDate.now(relogio.withZone(SAO_PAULO));
    }
}
