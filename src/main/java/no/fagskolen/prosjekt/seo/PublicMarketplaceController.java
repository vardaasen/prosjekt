package no.fagskolen.prosjekt.seo;

import no.fagskolen.prosjekt.marketplace.catalogue.PublishedListingCatalogue;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletResponse;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;

import java.util.Locale;

@Controller
public class PublicMarketplaceController {

    private final PublishedListingCatalogue catalogue;
    private final String publicBaseUrl;

    public PublicMarketplaceController(
            PublishedListingCatalogue catalogue,
            @Value("${app.public-base-url:http://localhost}") String publicBaseUrl) {
        this.catalogue = catalogue;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Havbruksbrukt");
        model.addAttribute("description", "B2B-markedsplass for brukt akvakulturutstyr.");
        model.addAttribute("listings", catalogue.findPublishedListings());
        model.addAttribute("robots", "index,follow");
        model.addAttribute("canonicalUrl", absoluteUrl("/"));
        return "home";
    }

    @GetMapping("/utstyr")
    public String catalogue(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String lokasjon,
            @RequestParam(defaultValue = "") String tilstand,
            Model model) {
        var condition = parseCondition(tilstand);
        var listings = catalogue.searchPublishedListings(q, lokasjon, condition);
        var filtered = !q.isBlank() || !lokasjon.isBlank() || !tilstand.isBlank();

        model.addAttribute("title", "Utforsk brukt akvakulturutstyr");
        model.addAttribute("description", "Søk i annonser for brukt utstyr til oppdrett og maritime virksomheter.");
        model.addAttribute("listings", listings);
        model.addAttribute("query", q);
        model.addAttribute("location", lokasjon);
        model.addAttribute("condition", tilstand);
        model.addAttribute("hasFilters", filtered);
        model.addAttribute("robots", filtered ? "noindex,follow" : "index,follow");
        model.addAttribute("canonicalUrl", absoluteUrl("/utstyr"));
        model.addAttribute("resultMessage", listings.isEmpty()
                ? "Ingen annonser matcher søket."
                : listings.size() + (listings.size() == 1 ? " annonse funnet." : " annonser funnet."));
        return "catalogue";
    }

    @GetMapping("/utstyr/{slug}")
    public String listing(
            @PathVariable String slug,
            Model model,
            HttpServletResponse response) {
        var listing = catalogue.findPublishedBySlug(slug);
        if (listing.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "error/404";
        }

        model.addAttribute("title", listing.get().title());
        model.addAttribute("description", listing.get().summary());
        model.addAttribute("listing", listing.get());
        model.addAttribute("robots", "index,follow");
        model.addAttribute("canonicalUrl", absoluteUrl("/utstyr/" + listing.get().slug()));
        return "listing";
    }

    @GetMapping("/selg")
    public String sell(Model model) {
        model.addAttribute("title", "Selg utstyr");
        model.addAttribute("description", "Søk om å selge brukt akvakulturutstyr på Havbruksbrukt.");
        model.addAttribute("robots", "noindex,follow");
        model.addAttribute("canonicalUrl", absoluteUrl("/selg"));
        return "sell";
    }

    @GetMapping("/tilgang-nektet")
    public String accessDenied(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("title", "Ingen tilgang");
        model.addAttribute("description", "Du har ikke tilgang til denne siden.");
        model.addAttribute("robots", "noindex,nofollow");
        model.addAttribute("canonicalUrl", absoluteUrl("/tilgang-nektet"));
        return "error/403";
    }

    @GetMapping("/innlogging-feilet")
    public String loginFailed(Model model) {
        model.addAttribute("title", "Innloggingen kunne ikke fullføres");
        model.addAttribute("description", "Innloggingen kunne ikke fullføres. Prøv igjen.");
        model.addAttribute("robots", "noindex,nofollow");
        model.addAttribute("canonicalUrl", absoluteUrl("/innlogging-feilet"));
        return "error/login-failed";
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        return ResponseEntity.ok("""
                User-agent: *
                Allow: /
                Disallow: /app
                Sitemap: %s
                """.formatted(absoluteUrl("/sitemap.xml")));
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        var urls = catalogue.findPublishedListings().stream()
                .map(listing -> """
                        <url><loc>%s</loc><lastmod>%s</lastmod></url>
                        """.formatted(absoluteUrl("/utstyr/" + listing.slug()), listing.publishedAt()))
                .toList();
        var body = """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url><loc>%s</loc></url>
                  <url><loc>%s</loc></url>
                  %s
                </urlset>
                """.formatted(absoluteUrl("/"), absoluteUrl("/utstyr"), String.join("\n  ", urls));
        return ResponseEntity.ok(body);
    }

    private static ListingCondition parseCondition(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ListingCondition.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ukjent tilstand");
        }
    }

    private String absoluteUrl(String path) {
        return publicBaseUrl + (path.isBlank() || "/".equals(path) ? "" : (path.startsWith("/") ? path : "/" + path));
    }
}
