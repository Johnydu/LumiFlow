package br.com.lumiflow.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Relógio de teste para {@code @Spy} + {@code @InjectMocks}. Sem instante fixo, acompanha o relógio do
 * sistema; com {@link #fixoEm(LocalDateTime)}, devolve sempre o mesmo instante na zona informada.
 */
public class RelogioTeste extends Clock {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private final ZoneId zona;
    private final Instant fixo;

    private RelogioTeste(ZoneId zona, Instant fixo) {
        this.zona = zona;
        this.fixo = fixo;
    }

    /** Acompanha o relógio do sistema, na zona de negócio. */
    public static RelogioTeste real() {
        return new RelogioTeste(ZONA, null);
    }

    public static RelogioTeste fixoEm(LocalDateTime dataHora) {
        return new RelogioTeste(ZONA, dataHora.atZone(ZONA).toInstant());
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId novaZona) {
        return new RelogioTeste(novaZona, fixo);
    }

    @Override
    public Instant instant() {
        return fixo != null ? fixo : Instant.now();
    }
}
