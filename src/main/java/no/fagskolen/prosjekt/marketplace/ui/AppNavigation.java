package no.fagskolen.prosjekt.marketplace.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Appmenyen (userflow 10): appfunksjoner etter rolle og vei tilbake til
 * nettstedet. Ruter er relative til appens rot (/app).
 */
record AppNavigation(List<Link> links) {

    record Link(String label, String href, boolean leavesApp) {
    }

    static AppNavigation forRoles(Set<String> roles) {
        // Markedsplassadministratoren er en egen innlogging som aldri handler og
        // ikke surfer (0010). Den står alltid i administrasjonen, så menyen har
        // ingen lenker, bare utlogging.
        if (roles.contains("ADMIN")) {
            return new AppNavigation(List.of());
        }
        var links = new ArrayList<Link>();
        links.add(new Link("Min side", "", false));
        // Gammel modell (0006) inntil utkast eies av virksomheten (#19, #26).
        if (roles.contains("SELLER")) {
            links.add(new Link("Selgerområde", "selgeromrade", false));
        }
        links.add(new Link("Til nettstedet", "/", true));
        return new AppNavigation(List.copyOf(links));
    }

    /** Min side er for personer som handler, ikke for markedsplassadministratoren. */
    static boolean mayUseMinSide(Set<String> roles) {
        return !roles.contains("ADMIN");
    }
}
