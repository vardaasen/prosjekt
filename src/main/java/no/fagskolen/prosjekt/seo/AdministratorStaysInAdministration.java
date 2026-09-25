package no.fagskolen.prosjekt.seo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Markedsplassadministratoren er en egen innlogging som ikke surfer (0010).
 * Offentlige sider sender den derfor til administrasjonen. Crawler-endepunkter,
 * feilsider og utlogging berøres ikke.
 */
@Configuration
class AdministratorStaysInAdministration implements WebMvcConfigurer, HandlerInterceptor {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/", "/utstyr", "/utstyr/**", "/selg", "/selgersoknad");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var administrator = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (administrator) {
            response.sendRedirect("/app/admin");
            return false;
        }
        return true;
    }
}
