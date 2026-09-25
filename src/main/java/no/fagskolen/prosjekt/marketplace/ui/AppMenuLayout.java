package no.fagskolen.prosjekt.marketplace.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import no.fagskolen.prosjekt.security.LogoutForm;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Felles layout for alle appsider med appmenyen (userflow 10). Vaadins
 * {@code @Layout} legger den rundt alle ruter automatisk. Vaadin sjekker
 * tilgang for layouten i tillegg til visningen, så layouten er åpen for alle
 * innloggede; hver visning avgjør selv hvem som slipper inn.
 */
@Layout
@PermitAll
public class AppMenuLayout extends AppLayout implements AfterNavigationObserver {

    private final Map<String, Anchor> appLinks = new HashMap<>();

    public AppMenuLayout(AuthenticationContext authenticationContext) {
        var roles = Stream.of("SELLER", "ADMIN")
                .filter(authenticationContext::hasRole)
                .collect(Collectors.toUnmodifiableSet());

        var menu = new Nav();
        menu.getElement().setAttribute("aria-label", "Appmeny");
        menu.addClassName("app-menu");
        for (var link : AppNavigation.forRoles(roles).links()) {
            var anchor = new Anchor(link.leavesApp() ? link.href() : "/app/" + link.href(), link.label());
            if (link.leavesApp()) {
                // Lenken går ut av Vaadin-appen til den offentlige flaten.
                anchor.setRouterIgnore(true);
            } else {
                appLinks.put(link.href(), anchor);
            }
            menu.add(anchor);
        }
        menu.add(new LogoutForm());

        var brand = new Span("Havbruksbrukt");
        brand.addClassName("app-brand");
        addToNavbar(brand, menu);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        var current = event.getLocation().getPath();
        appLinks.forEach((href, anchor) -> {
            if (href.equals(current)) {
                anchor.getElement().setAttribute("aria-current", "page");
            } else {
                anchor.getElement().removeAttribute("aria-current");
            }
        });
    }
}
