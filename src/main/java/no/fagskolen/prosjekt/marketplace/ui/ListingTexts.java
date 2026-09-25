package no.fagskolen.prosjekt.marketplace.ui;

import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;

/** Publiseringsstatus med ord (designhåndboken), felles for Min side og administrasjonen. */
public final class ListingTexts {

    private ListingTexts() {
    }

    public static String status(ListingPublicationStatus status) {
        return switch (status) {
            case DRAFT -> "Utkast";
            case SUBMITTED -> "Til godkjenning";
            case PUBLISHED -> "Publisert";
            case ARCHIVED -> "Arkivert";
        };
    }
}
