package com.superfercho.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * HTTP matcher for catalog GET routes. Distinguishes {@code view=ADMIN} from public catalog reads
 * ({@code view} absent or {@code PUBLIC}) without inspecting controllers.
 */
public final class CatalogGetRequestMatcher implements RequestMatcher {

    private static final PathPatternParser PATH_PARSER = new PathPatternParser();
    private static final PathPattern CATEGORIES = PATH_PARSER.parse("/api/v1/categories/**");
    private static final PathPattern PRODUCTS = PATH_PARSER.parse("/api/v1/products/**");
    private static final String VIEW_PARAMETER = "view";
    private static final String ADMIN_VIEW = "ADMIN";

    private final boolean adminView;

    private CatalogGetRequestMatcher(boolean adminView) {
        this.adminView = adminView;
    }

    public static CatalogGetRequestMatcher adminView() {
        return new CatalogGetRequestMatcher(true);
    }

    public static CatalogGetRequestMatcher publicView() {
        return new CatalogGetRequestMatcher(false);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod()) || !isCatalogPath(path(request))) {
            return false;
        }
        boolean requestedAdmin = ADMIN_VIEW.equals(request.getParameter(VIEW_PARAMETER));
        return adminView == requestedAdmin;
    }

    private static boolean isCatalogPath(String path) {
        PathContainer container = PathContainer.parsePath(path);
        return CATEGORIES.matches(container) || PRODUCTS.matches(container);
    }

    private static String path(HttpServletRequest request) {
        String servletPath = request.getServletPath() == null ? "" : request.getServletPath();
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            return servletPath + pathInfo;
        }
        if (!servletPath.isEmpty()) {
            return servletPath;
        }
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
