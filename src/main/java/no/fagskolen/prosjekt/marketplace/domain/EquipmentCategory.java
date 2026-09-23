package no.fagskolen.prosjekt.marketplace.domain;

public enum EquipmentCategory {
    PUMP("Pumpe"),
    NET("Not/maske"),
    FEED_SYSTEM("Fôringssystem"),
    FLOATING_STRUCTURE("Flytende struktur"),
    OTHER("Annet");

    private final String label;

    EquipmentCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
