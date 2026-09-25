package no.fagskolen.prosjekt.marketplace.people;

/**
 * Navn og e-post for en person, slått opp i identitetsleverandøren når det
 * trengs. Lagres aldri i markedsplassens database (0009).
 */
public record PersonContact(String displayName, String email) {
}
