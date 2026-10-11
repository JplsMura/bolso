package dev.bolso.lancamentos.adapter.out.relogio;

import dev.bolso.lancamentos.application.Relogio;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
class RelogioSistema implements Relogio {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final Clock relogio = Clock.systemUTC();

    @Override
    public Instant agora() {
        return relogio.instant();
    }

    @Override
    public YearMonth mesAtual() {
        return YearMonth.now(relogio.withZone(SAO_PAULO));
    }
}
