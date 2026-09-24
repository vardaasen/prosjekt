package no.fagskolen.prosjekt.seo;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hovedmenyen på de offentlige sidene, avhengig av innlogging og
 * administratorrolle (userflow 10). Søkemotorer er alltid anonyme og ser
 * utlogget meny.
 */
public record PublicNavigation(List<Link> links, boolean loggedIn, String returnTo) {

    public record Link(String label, String href, boolean current) {
    }

    public static PublicNavigation forVisitor(Authentication authentication, String currentPath) {
        var loggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        var administrator = loggedIn && roles(authentication).contains("ROLE_ADMIN");

        // Kjøper og selger er ikke kontotyper (beslutningsnotat 0007), så menyen
        // skiller bare på innlogget og markedsplassadministrator. «Min side» (/app)
        // legges til når appen er åpnet for alle innloggede personer; menyen skal
        // aldri lenke til en side personen får 403 på.
        var links = new ArrayList<Link>();
        links.add(link("Utforsk utstyr", "/utstyr", currentPath));
        links.add(link("Selg utstyr", "/selg", currentPath));
        if (administrator) {
            links.add(link("Administrasjon", "/app/admin", currentPath));
        }
        return new PublicNavigation(List.copyOf(links), loggedIn, currentPath);
    }

    private static Set<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private static Link link(String label, String href, String currentPathAndQuery) {
        var queryStart = currentPathAndQuery.indexOf('?');
        var path = queryStart < 0 ? currentPathAndQuery : currentPathAndQuery.substring(0, queryStart);
        var current = path.equals(href) || path.startsWith(href + "/");
        return new Link(label, href, current);
    }
}
