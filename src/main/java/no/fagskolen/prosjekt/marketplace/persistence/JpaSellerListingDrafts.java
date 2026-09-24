package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.drafts.ListingDraft;
import no.fagskolen.prosjekt.marketplace.drafts.SellerListingDrafts;
import no.fagskolen.prosjekt.marketplace.domain.Listing;
import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;
import no.fagskolen.prosjekt.marketplace.domain.SellerAccount;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

@Service
class JpaSellerListingDrafts implements SellerListingDrafts {

    private final ListingJpaRepository listings;
    private final SellerAccountJpaRepository sellerAccounts;
    private final ListingDataMapper mapper;

    JpaSellerListingDrafts(
            ListingJpaRepository listings,
            SellerAccountJpaRepository sellerAccounts,
            ListingDataMapper mapper) {
        this.listings = listings;
        this.sellerAccounts = sellerAccounts;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Listing createDraft(SellerAccount owner, ListingDraft draft) {
        var sellerAccount = sellerAccounts.findById(owner.oidcSubject())
                .orElseThrow(() -> new IllegalStateException("Seller account is not registered"));
        var listing = new ListingJpaEntity(
                draft.slug(),
                draft.title(),
                draft.location(),
                draft.condition(),
                draft.category(),
                ListingPublicationStatus.DRAFT,
                draft.priceNok(),
                sellerAccount.sellerName(),
                sellerAccount.verifiedSeller(),
                sellerAccount.sellerLocation(),
                sellerAccount,
                LocalDate.now(),
                draft.summary(),
                new LinkedHashSet<>());
        try {
            return mapper.toDomain(listings.saveAndFlush(listing));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("A listing with this slug already exists", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> findDrafts(SellerAccount owner) {
        return listings.findAllBySellerAccountOidcSubjectAndPublicationStatusOrderByIdDesc(
                        owner.oidcSubject(),
                        ListingPublicationStatus.DRAFT)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Listing publishDraft(SellerAccount owner, String slug) {
        var draft = listings.findBySlugAndSellerAccountOidcSubjectAndPublicationStatus(
                        slug,
                        owner.oidcSubject(),
                        ListingPublicationStatus.DRAFT)
                .orElseThrow(() -> new IllegalArgumentException("Draft is not available for this seller"));
        draft.publish(LocalDate.now());
        return mapper.toDomain(draft);
    }
}
