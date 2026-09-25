package no.fagskolen.prosjekt.marketplace.domain;

/**
 * Tilknytningens livssyklus (domenemodellen, userflow 11): venter, verifisert,
 * avvist («aldri godtatt») eller trukket tilbake («ikke lenger godtatt»).
 */
public enum AffiliationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    WITHDRAWN
}
