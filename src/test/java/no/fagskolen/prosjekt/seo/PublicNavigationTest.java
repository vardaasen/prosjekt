package no.fagskolen.prosjekt.seo;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PublicNavigationTest {

    @Test
    void offersLoginToAnonymousVisitors() {
        var navigation = PublicNavigation.forVisitor(anonymous(), "/utstyr/sentrifugalpumpe-450");

        assertThat(labels(navigation)).containsExactly("Utforsk utstyr", "Selg utstyr");
        assertThat(navigation.loggedIn()).isFalse();
    }

    @Test
    void treatsMissingAuthenticationAsAnonymous() {
        assertThat(PublicNavigation.forVisitor(null, "/").loggedIn()).isFalse();
    }

    @Test
    void showsTheSameMenuToEveryLoggedInPersonRegardlessOfTradingRoles() {
        for (var roles : new String[][] {{}, {"BUYER"}, {"SELLER"}}) {
            var navigation = PublicNavigation.forVisitor(withRoles(roles), "/");

            assertThat(labels(navigation)).containsExactly("Utforsk utstyr", "Selg utstyr", "Min side");
            assertThat(navigation.loggedIn()).isTrue();
        }
    }

    @Test
    void addsAdministrationForMarketplaceAdministrators() {
        var navigation = PublicNavigation.forVisitor(withRoles("ADMIN"), "/");

        assertThat(navigation.links()).extracting(PublicNavigation.Link::label, PublicNavigation.Link::href)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Utforsk utstyr", "/utstyr"),
                        org.assertj.core.groups.Tuple.tuple("Selg utstyr", "/selg"),
                        org.assertj.core.groups.Tuple.tuple("Min side", "/app"),
                        org.assertj.core.groups.Tuple.tuple("Administrasjon", "/app/admin"));
    }

    @Test
    void marksTheCurrentSection() {
        var navigation = PublicNavigation.forVisitor(anonymous(), "/utstyr/sentrifugalpumpe-450");

        assertThat(navigation.links()).filteredOn(PublicNavigation.Link::current)
                .extracting(PublicNavigation.Link::label)
                .containsExactly("Utforsk utstyr");
    }

    @Test
    void marksTheCurrentSectionWhenFiltersAreApplied() {
        var navigation = PublicNavigation.forVisitor(anonymous(), "/utstyr?q=pumpe");

        assertThat(navigation.links()).filteredOn(PublicNavigation.Link::current)
                .extracting(PublicNavigation.Link::label)
                .containsExactly("Utforsk utstyr");
    }

    private static java.util.List<String> labels(PublicNavigation navigation) {
        return navigation.links().stream().map(PublicNavigation.Link::label).toList();
    }

    private static Authentication anonymous() {
        return new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

    private static Authentication withRoles(String... roles) {
        var authorities = java.util.Arrays.stream(roles).map(role -> "ROLE_" + role).toArray(String[]::new);
        var authentication = new TestingAuthenticationToken("demo", "n/a", authorities);
        authentication.setAuthenticated(true);
        return authentication;
    }
}
