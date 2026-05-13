package com.company.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servlet filter that authenticates requests using a Bearer JWT in the {@code Authorization} header.
 *
 * <p>On each request, extracts the token, validates it via {@link JwtTokenProvider}, and — when
 * the token is a valid access token — populates the {@link org.springframework.security.core.context.SecurityContext}
 * with a {@link UserPrincipal}-based authentication. Invalid or missing tokens are silently ignored;
 * unauthenticated requests to protected endpoints are rejected downstream by the security filter chain.
 *
 * <p>Extends {@link org.springframework.web.filter.OncePerRequestFilter} to ensure single execution per request.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Extracts the Bearer token from {@code Authorization}, validates it, and populates the
     * {@code SecurityContext}. Forwards the request to the next filter regardless of outcome.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractBearerToken(request);
        if (token != null) {
            try {
                Jws<Claims> jws = jwtTokenProvider.parseAndValidate(token);
                Claims claims = jws.getPayload();

                if (!"access".equals(claims.get("type"))) {
                    filterChain.doFilter(request, response);
                    return;
                }

                List<String> roles = jwtTokenProvider.extractRoles(claims);
                String role = roles.isEmpty() ? "ROLE_USER" : roles.get(0);

                UserPrincipal principal = new UserPrincipal(
                        jwtTokenProvider.extractUserId(claims),
                        null, null, role, true, null
                );

                var authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException ignored) {
                // Invalid token — EntryPoint will return 401 for protected endpoints
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
