package no.fagskolen.prosjekt.seo;

import no.fagskolen.prosjekt.marketplace.service.ListingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PublicMarketplaceController {

    private final ListingService listingService;

    public PublicMarketplaceController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Havbruksbrukt");
        model.addAttribute("description", "B2B-markedsplass for brukt akvakulturutstyr.");
        model.addAttribute("listings", listingService.findPublicListings());
        return "home";
    }

    @GetMapping("/utstyr")
    public String catalogue(Model model) {
        model.addAttribute("title", "Utforsk brukt akvakulturutstyr");
        model.addAttribute("description", "Søk i annonser for brukt utstyr til oppdrett og maritime virksomheter.");
        model.addAttribute("listings", listingService.findPublicListings());
        return "catalogue";
    }

    @GetMapping("/utstyr/{slug}")
    public String listing(@PathVariable String slug, Model model) {
        var listing = listingService.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("title", listing.title());
        model.addAttribute("description", listing.summary());
        model.addAttribute("listing", listing);
        return "listing";
    }
}
