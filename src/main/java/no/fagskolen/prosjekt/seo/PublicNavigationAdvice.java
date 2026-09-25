package no.fagskolen.prosjekt.seo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = PublicMarketplaceController.class)
class PublicNavigationAdvice {

    @ModelAttribute("navigation")
    PublicNavigation navigation(Authentication authentication, HttpServletRequest request) {
        var query = request.getQueryString();
        var currentPath = request.getRequestURI() + (query == null ? "" : "?" + query);
        return PublicNavigation.forVisitor(authentication, currentPath);
    }
}
