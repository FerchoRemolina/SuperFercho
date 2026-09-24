package com.superfercho.identity.infrastructure.security;

import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.domain.model.CustomerPreview;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Invalidates cryptographically valid JWTs whose storefront preview is closed or expired.
 */
public final class StorefrontPreviewGateFilter extends OncePerRequestFilter {

    private final CustomerPreviewRepository customerPreviewRepository;
    private final Clock clock;
    private final SecurityProblemDetailResponses problemResponses;

    public StorefrontPreviewGateFilter(
            CustomerPreviewRepository customerPreviewRepository,
            Clock clock,
            SecurityProblemDetailResponses problemResponses) {
        this.customerPreviewRepository = customerPreviewRepository;
        this.clock = clock;
        this.problemResponses = problemResponses;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal
                && principal.isStorefrontPreview()) {
            Optional<CustomerPreview> preview =
                    customerPreviewRepository.findById(principal.previewId());
            if (preview.isEmpty()
                    || !preview.get().temporaryCustomerId().equals(principal.userId())
                    || !preview.get().isUsable(clock.instant())) {
                SecurityContextHolder.clearContext();
                problemResponses.writeUnauthenticated(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
