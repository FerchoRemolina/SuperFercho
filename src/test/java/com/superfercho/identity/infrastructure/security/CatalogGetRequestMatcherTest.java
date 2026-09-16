package com.superfercho.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CatalogGetRequestMatcherTest {

    private final CatalogGetRequestMatcher publicView = CatalogGetRequestMatcher.publicView();
    private final CatalogGetRequestMatcher adminView = CatalogGetRequestMatcher.adminView();

    @Test
    void shouldTreatMissingViewAsPublicCatalogGet() {
        MockHttpServletRequest categories = catalogGet("/api/v1/categories");
        MockHttpServletRequest products = catalogGet("/api/v1/products");
        MockHttpServletRequest search = catalogGet("/api/v1/products/search");

        assertTrue(publicView.matches(categories));
        assertTrue(publicView.matches(products));
        assertTrue(publicView.matches(search));
        assertFalse(adminView.matches(categories));
        assertFalse(adminView.matches(products));
        assertFalse(adminView.matches(search));
    }

    @Test
    void shouldTreatPublicViewAsPublicCatalogGet() {
        MockHttpServletRequest request = catalogGet("/api/v1/categories");
        request.setParameter("view", "PUBLIC");

        assertTrue(publicView.matches(request));
        assertFalse(adminView.matches(request));
    }

    @Test
    void shouldTreatAdminViewAsAdminCatalogGet() {
        MockHttpServletRequest categories = catalogGet("/api/v1/categories");
        categories.setParameter("view", "ADMIN");
        MockHttpServletRequest products = catalogGet("/api/v1/products");
        products.setParameter("view", "ADMIN");
        MockHttpServletRequest search = catalogGet("/api/v1/products/search");
        search.setParameter("view", "ADMIN");

        assertTrue(adminView.matches(categories));
        assertTrue(adminView.matches(products));
        assertTrue(adminView.matches(search));
        assertFalse(publicView.matches(categories));
        assertFalse(publicView.matches(products));
        assertFalse(publicView.matches(search));
    }

    @Test
    void shouldIgnoreNonGetCatalogRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/categories");
        request.setServletPath("/api/v1/categories");
        request.setParameter("view", "ADMIN");

        assertFalse(publicView.matches(request));
        assertFalse(adminView.matches(request));
    }

    @Test
    void shouldIgnoreNonCatalogGets() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.setServletPath("/api/v1/orders");
        request.setParameter("view", "ADMIN");

        assertFalse(publicView.matches(request));
        assertFalse(adminView.matches(request));
    }

    private static MockHttpServletRequest catalogGet(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }
}
