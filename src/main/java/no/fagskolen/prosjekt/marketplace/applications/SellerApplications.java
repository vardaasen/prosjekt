package no.fagskolen.prosjekt.marketplace.applications;

import no.fagskolen.prosjekt.marketplace.domain.Seller;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplication;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplicationAuditEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerApplications {

    SellerApplication submit(String oidcSubject, Seller seller);

    Optional<SellerApplication> findLatestFor(String oidcSubject);

    List<SellerApplication> findPending();

    List<SellerApplicationAuditEntry> findAuditTrail(UUID applicationId);

    SellerApplication approve(UUID applicationId, String administratorSubject);

    SellerApplication reject(UUID applicationId, String administratorSubject, String reason);
}
