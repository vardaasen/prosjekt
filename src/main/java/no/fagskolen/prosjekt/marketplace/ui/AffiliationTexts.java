package no.fagskolen.prosjekt.marketplace.ui;

import no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Felles tekster for tilknytninger i appen, slik at Min side og
 * administrasjonen bruker de samme ordene (designhåndboken: status med ord).
 */
public final class AffiliationTexts {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Oslo"));

    private AffiliationTexts() {
    }

    public static String status(AffiliationStatus status) {
        return switch (status) {
            case PENDING -> "Venter på verifisering";
            case VERIFIED -> "Verifisert";
            case REJECTED -> "Avvist";
            case WITHDRAWN -> "Trukket tilbake";
        };
    }

    public static String time(Instant instant) {
        return instant == null ? "" : TIME.format(instant);
    }
}
