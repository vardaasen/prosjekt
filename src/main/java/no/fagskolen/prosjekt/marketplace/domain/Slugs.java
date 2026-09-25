package no.fagskolen.prosjekt.marketplace.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Lesbare, unike adresser for annonser, laget fra tittelen. Personen skriver
 * ikke slug selv (#19).
 */
public final class Slugs {

    private Slugs() {
    }

    public static String fromTitle(String title, String uniqueSuffix) {
        var text = title.toLowerCase(Locale.ROOT)
                .replace("æ", "ae")
                .replace("ø", "o")
                .replace("å", "a");
        text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        text = text.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (text.length() > 60) {
            text = text.substring(0, 60).replaceAll("-+$", "");
        }
        return (text.isEmpty() ? "annonse" : text) + "-" + uniqueSuffix;
    }
}
