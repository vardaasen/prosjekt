package no.fagskolen.prosjekt;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;

/**
 * Tema for Vaadin-appen. Vaadin 25 laster ikke noe tema av seg selv; Aura med
 * designhåndbokens palett står i META-INF/resources/styles.css.
 */
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
public class AppShell implements AppShellConfigurator {
}
