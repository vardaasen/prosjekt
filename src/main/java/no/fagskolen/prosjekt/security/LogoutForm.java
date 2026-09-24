package no.fagskolen.prosjekt.security;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinServletRequest;
import org.springframework.security.web.csrf.CsrfToken;

/**
 * Utloggingsknapp for Vaadin-flatene som sender et vanlig {@code POST /logout}
 * med Spring Securitys CSRF-token. Utlogging via GET ville latt et annet
 * nettsted logge brukeren ut av både markedsplassen og Keycloak-SSO-økten med
 * en toppnivå-navigasjon. Et vanlig skjema fanges heller ikke opp av Vaadins
 * klientsideruter, slik en {@code <a href="/logout">} ble.
 */
@Tag("form")
public class LogoutForm extends Component {

    public LogoutForm() {
        this(currentCsrfToken());
    }

    LogoutForm(CsrfToken csrfToken) {
        getElement().setAttribute("method", "post");
        getElement().setAttribute("action", "/logout");

        if (csrfToken != null) {
            getElement().appendChild(new Element("input")
                    .setAttribute("type", "hidden")
                    .setAttribute("name", csrfToken.getParameterName())
                    .setAttribute("value", csrfToken.getToken()));
        }

        getElement().appendChild(new Element("button")
                .setAttribute("type", "submit")
                .setText("Logg ut"));
    }

    private static CsrfToken currentCsrfToken() {
        var request = VaadinServletRequest.getCurrent();
        if (request == null) {
            return null;
        }
        return request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token ? token : null;
    }
}
