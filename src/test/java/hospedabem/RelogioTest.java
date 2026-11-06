package hospedabem;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelogioTest {
    @Test
    void estacaoAtrasadaPassaAUsarAHoraDoBanco() {
        LocalDateTime local = LocalDateTime.of(2026, 11, 6, 23, 58);
        Clock estacao = Clock.fixed(local.toInstant(ZoneOffset.ofHours(-4)), ZoneId.of("America/Campo_Grande"));
        LocalDateTime banco = local.plusMinutes(3);
        assertEquals(banco, LocalDateTime.now(App.alinhado(estacao, banco)));
    }
}
