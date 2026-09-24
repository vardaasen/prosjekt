package no.fagskolen.prosjekt.security;

import com.vaadin.flow.dom.Element;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.assertj.core.api.Assertions.assertThat;

class LogoutFormTest {

    @Test
    void postsToLogoutWithCsrfToken() {
        var form = new LogoutForm(new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-value")).getElement();

        assertThat(form.getTag()).isEqualTo("form");
        assertThat(form.getAttribute("method")).isEqualTo("post");
        assertThat(form.getAttribute("action")).isEqualTo("/logout");

        Element csrfInput = form.getChild(0);
        assertThat(csrfInput.getTag()).isEqualTo("input");
        assertThat(csrfInput.getAttribute("type")).isEqualTo("hidden");
        assertThat(csrfInput.getAttribute("name")).isEqualTo("_csrf");
        assertThat(csrfInput.getAttribute("value")).isEqualTo("token-value");

        Element button = form.getChild(1);
        assertThat(button.getTag()).isEqualTo("button");
        assertThat(button.getAttribute("type")).isEqualTo("submit");
        assertThat(button.getText()).isEqualTo("Logg ut");
    }

    @Test
    void omitsCsrfInputWhenNoTokenIsAvailable() {
        var form = new LogoutForm(null).getElement();

        assertThat(form.getChildCount()).isEqualTo(1);
        assertThat(form.getChild(0).getTag()).isEqualTo("button");
    }
}
