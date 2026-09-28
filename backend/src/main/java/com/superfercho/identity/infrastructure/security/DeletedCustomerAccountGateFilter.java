package com.superfercho.identity.infrastructure.security;

import com.superfercho.identity.application.port.CustomerAccountAccessPort;
import com.superfercho.identity.domain.model.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects cryptographically valid CUSTOMER JWTs whose account has {@code deletedAt != null} (or the
 * user no longer exists). Skips ADMIN and storefront Preview principals.
 */
public final class DeletedCustomerAccountGateFilter extends OncePerRequestFilter {

    private final CustomerAccountAccessPort customerAccountAccessPort;
    private final SecurityProblemDetailResponses problemResponses;

    public DeletedCustomerAccountGateFilter(
            CustomerAccountAccessPort customerAccountAccessPort,
            SecurityProblemDetailResponses problemResponses) {
        this.customerAccountAccessPort = customerAccountAccessPort;
        this.problemResponses = problemResponses;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal
                && principal.role() == Role.CUSTOMER
                && !principal.isStorefrontPreview()
                && !customerAccountAccessPort.allowsCustomerAccess(principal.userId())) {
            SecurityContextHolder.clearContext();
            problemResponses.writeUnauthenticated(response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
