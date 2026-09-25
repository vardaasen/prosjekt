package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus;
import no.fagskolen.prosjekt.marketplace.domain.ListingContent;
import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;
import no.fagskolen.prosjekt.marketplace.domain.Slugs;
import no.fagskolen.prosjekt.marketplace.listings.BusinessListing;
import no.fagskolen.prosjekt.marketplace.listings.BusinessListings;
import no.fagskolen.prosjekt.marketplace.listings.ListingAuditEntry;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
class JpaBusinessListings implements BusinessListings {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ZoneId NORWAY = ZoneId.of("Europe/Oslo");

    private final ListingJpaRepository listings;
    private final BusinessJpaRepository businesses;
    private final AffiliationJpaRepository affiliations;
    private final ListingAuditJpaRepository auditTrail;
    private final Clock clock;

    JpaBusinessListings(
            ListingJpaRepository listings,
            BusinessJpaRepository businesses,
            AffiliationJpaRepository affiliations,
            ListingAuditJpaRepository auditTrail) {
        this.listings = listings;
        this.businesses = businesses;
        this.affiliations = affiliations;
        this.auditTrail = auditTrail;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional
    public BusinessListing createDraft(String personSubject, UUID businessId, ListingContent content) {
        var affiliation = draftingAffiliation(personSubject, businessId);
        var business = businesses.findById(businessId).orElseThrow();
        var draft = listings.saveAndFlush(ListingJpaEntity.draftFor(business, affiliation,
                Slugs.fromTitle(content.title(), uniqueSuffix()), content));
        audit(draft, ListingAuditEntry.Action.CREATED, affiliation.personSubject(), null);
        return toBusinessListing(draft);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessListing> findForPerson(String personSubject) {
        return listings.findForPerson(personSubject.trim()).stream().map(JpaBusinessListings::toBusinessListing).toList();
    }

    @Override
    @Transactional
    public BusinessListing updateDraft(String personSubject, long listingId, ListingContent content) {
        var listing = find(listingId);
        var affiliation = draftingAffiliation(personSubject, listing.business().id());
        listing.publicationStatus().edit();
        listing.updateContent(content);
        var saved = save(listing);
        audit(listing, ListingAuditEntry.Action.UPDATED, affiliation.personSubject(), null);
        return saved;
    }

    @Override
    @Transactional
    public BusinessListing submitForApproval(String personSubject, long listingId, String reviewNote) {
        var listing = find(listingId);
        var submitter = affiliations.findForPersonAndBusiness(personSubject.trim(), listing.business().id())
                .orElseThrow(() -> new IllegalStateException("Du har ingen tilknytning til virksomheten som eier annonsen."));
        var newStatus = listing.publicationStatus().submit(submitter.status());
        var creator = listing.createdThroughAffiliation();
        var fromUnverifiedColleague = creator != null
                && !creator.personSubject().equals(submitter.personSubject())
                && creator.status() != AffiliationStatus.VERIFIED;
        var note = reviewNote == null || reviewNote.isBlank() ? null : reviewNote.trim();
        if (fromUnverifiedColleague && note == null) {
            throw new IllegalStateException("Utkastet er laget av en person som ikke lenger er verifisert. "
                    + "Skriv en merknad om hva du har kontrollert før du sender det.");
        }
        listing.submit(newStatus, clock.instant());
        var saved = save(listing);
        audit(listing, fromUnverifiedColleague
                        ? ListingAuditEntry.Action.SUBMITTED_FOR_UNVERIFIED_CREATOR
                        : ListingAuditEntry.Action.SUBMITTED,
                submitter.personSubject(), fromUnverifiedColleague ? note : null);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessListing> findAwaitingApproval() {
        return listings.findAwaitingApproval().stream().map(JpaBusinessListings::toBusinessListing).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessListing> findAll() {
        return listings.findAllOwnedByBusinesses().stream().map(JpaBusinessListings::toBusinessListing).toList();
    }

    @Override
    @Transactional
    public BusinessListing approve(long listingId, String administratorSubject) {
        var listing = find(listingId);
        var now = clock.instant();
        listing.approve(listing.publicationStatus().approve(), administratorSubject, now, LocalDate.ofInstant(now, NORWAY));
        var saved = save(listing);
        audit(listing, ListingAuditEntry.Action.APPROVED, administratorSubject, null);
        return saved;
    }

    @Override
    @Transactional
    public BusinessListing returnForChanges(long listingId, String administratorSubject, String feedback) {
        var listing = find(listingId);
        listing.returnForChanges(listing.publicationStatus().returnForChanges(feedback), administratorSubject,
                clock.instant(), feedback);
        var saved = save(listing);
        audit(listing, ListingAuditEntry.Action.RETURNED_FOR_CHANGES, administratorSubject, feedback.trim());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAuditEntry> findAuditTrail(long listingId) {
        return auditTrail.findByListingIdOrderByOccurredAtAscIdAsc(listingId).stream()
                .map(ListingAuditJpaEntity::toDomain)
                .toList();
    }

    private AffiliationJpaEntity draftingAffiliation(String personSubject, UUID businessId) {
        return affiliations.findForPersonAndBusiness(personSubject.trim(), businessId)
                .filter(candidate -> ListingPublicationStatus.mayCreateDraft(candidate.status()))
                .orElseThrow(() -> new IllegalStateException(
                        "Tilknytningen din til virksomheten må være verifisert før du kan lage eller endre en annonse."));
    }

    private ListingJpaEntity find(long listingId) {
        return listings.findWithBusiness(listingId)
                .orElseThrow(() -> new IllegalStateException("Fant ikke annonsen."));
    }

    private BusinessListing save(ListingJpaEntity listing) {
        try {
            return toBusinessListing(listings.saveAndFlush(listing));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new IllegalStateException("Annonsen ble nettopp endret av noen andre. Oppdater siden.", exception);
        }
    }

    private void audit(ListingJpaEntity listing, ListingAuditEntry.Action action, String actorSubject, String note) {
        auditTrail.save(new ListingAuditJpaEntity(listing.id(), action, actorSubject, note, clock.instant()));
    }

    private static String uniqueSuffix() {
        return Integer.toString(RANDOM.nextInt(36 * 36 * 36 * 36), 36);
    }

    private static BusinessListing toBusinessListing(ListingJpaEntity entity) {
        var business = entity.business();
        var creator = entity.createdThroughAffiliation();
        return new BusinessListing(
                entity.id(),
                entity.slug(),
                business.id(),
                business.name(),
                entity.title(),
                entity.location(),
                entity.condition(),
                entity.category(),
                entity.priceNok(),
                entity.summary(),
                entity.publicationStatus(),
                entity.reviewFeedback(),
                creator == null ? null : creator.personSubject(),
                creator != null && creator.status() == AffiliationStatus.VERIFIED);
    }
}
