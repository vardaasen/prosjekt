package no.fagskolen.prosjekt.marketplace.ui;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Appmenyen (userflow 10): appfunksjoner etter rolle, pluss vei tilbake til
 * nettstedet. Utlogging ligger ved siden av menyen.
 */
class AppNavigationTest {

    @Test
    void offersMinSideAndTheWayBackToEveryLoggedInPerson() {
        assertThat(AppNavigation.forRoles(Set.of()).links())
                .extracting(AppNavigation.Link::label, AppNavigation.Link::href, AppNavigation.Link::leavesApp)
                .containsExactly(
                        tuple("Min side", "", false),
                        tuple("Til nettstedet", "/", true));
    }

    @Test
    void givesMarketplaceAdministratorsNoLinksOnlyLogout() {
        // Egen innlogging som aldri handler og ikke surfer (0010); en lenke til
        // siden administratoren allerede står på har ingen hensikt.
        assertThat(AppNavigation.forRoles(Set.of("ADMIN")).links()).isEmpty();
        assertThat(AppNavigation.forRoles(Set.of("ADMIN", "SELLER")).links()).isEmpty();
    }

    @Test
    void keepsTheOldSellerAreaReachableForSellersUntilTheMigration() {
        assertThat(AppNavigation.forRoles(Set.of("SELLER")).links())
                .extracting(AppNavigation.Link::label, AppNavigation.Link::href)
                .containsExactly(
                        tuple("Min side", ""),
                        tuple("Selgerområde", "selgeromrade"),
                        tuple("Til nettstedet", "/"));
    }

    @Test
    void sendsMarketplaceAdministratorsAwayFromMinSide() {
        assertThat(AppNavigation.mayUseMinSide(Set.of("ADMIN"))).isFalse();
        assertThat(AppNavigation.mayUseMinSide(Set.of("SELLER"))).isTrue();
        assertThat(AppNavigation.mayUseMinSide(Set.of())).isTrue();
    }
}
