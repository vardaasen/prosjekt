package no.fagskolen.prosjekt.marketplace.domain;

public enum ListingCondition {
    GOOD("God stand"),
    USED("Brukt"),
    NEEDS_SERVICE("Trenger service");

    private final String displayName;

    ListingCondition(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
