package com.dotechly.gotechly.core.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;

/**
 * Validates the {@code X-Internal-Api-Key} header on every request.
 * Uses constant-time comparison to prevent timing attacks.
 * <p>
 * When the key matches, sets an {@link InternalServiceAuthentication} on the
 * SecurityContext so Spring Security's {@code .authenticated()} checks pass.
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(InternalApiKeyFilter.class);
    public static final String AUTH_USER_HEADER = "X-Auth-User";

    private final String headerName;
    private final byte[] expectedSecret;
    private final boolean enabled;

    public InternalApiKeyFilter(InternalApiKeyProperties properties) {
        this.headerName = properties.getHeaderName();
        this.expectedSecret = properties.getSecret() != null
                ? properties.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8)
                : new byte[0];
        this.enabled = properties.isEnabled();
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(headerName);

        if (providedKey == null || providedKey.isBlank()) {
            LOG.debug("Missing {} header from {}", headerName, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing internal API key\"}");
            return;
        }

        byte[] providedBytes = providedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedSecret, providedBytes)) {
            LOG.warn("Invalid {} header from {}", headerName, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid internal API key\"}");
            return;
        }

        // Key is valid — set authentication so Spring Security passes.
        // If Gateway forwarded X-Auth-User, use the upstream user as the principal name
        // so service-layer audit fields (createdBy, makerId, etc.) reflect the actual user.
        String authUser = request.getHeader(AUTH_USER_HEADER);
        String principal = (authUser != null && !authUser.isBlank())
                ? authUser
                : "internal-service@" + request.getRemoteAddr();

        SecurityContextHolder.getContext().setAuthentication(
                new InternalServiceAuthentication(principal));

        filterChain.doFilter(request, response);
    }

    /**
     * Lightweight authentication token for requests that pass the API key check.
     */
    static class InternalServiceAuthentication extends AbstractAuthenticationToken {

        private final String principal;

        InternalServiceAuthentication(String principal) {
            super(AuthorityUtils.createAuthorityList("ROLE_INTERNAL_SERVICE"));
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

        @Override
        public String getName() {
            return principal;
        }
    }
}
