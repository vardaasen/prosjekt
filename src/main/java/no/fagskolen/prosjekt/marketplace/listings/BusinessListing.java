package no.fagskolen.prosjekt.marketplace.listings;

import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * En annonse slik virksomheten og administratoren ser den, i alle statuser.
 * {@code feedback} er administratorens tilbakemelding når annonsen er sendt
 * tilbake til utkast.
 */
public record BusinessListing(
        long id,
        String slug,
        UUID businessId,
        String businessName,
        String title,
        String location,
        ListingCondition condition,
        EquipmentCategory category,
        BigDecimal priceNok,
        String summary,
        ListingPublicationStatus status,
        String feedback,
        String createdBySubject,
        boolean createdByVerified) {
}
