package no.fagskolen.prosjekt.marketplace.drafts;

import no.fagskolen.prosjekt.marketplace.domain.Listing;
import no.fagskolen.prosjekt.marketplace.domain.SellerAccount;

import java.util.List;

public interface SellerListingDrafts {

    Listing createDraft(SellerAccount owner, ListingDraft draft);

    List<Listing> findDrafts(SellerAccount owner);
}
