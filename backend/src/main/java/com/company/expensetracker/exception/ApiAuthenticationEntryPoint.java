package com.company.expensetracker.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Spring Security {@link AuthenticationEntryPoint} that returns an RFC 7807 Problem Details
 * response ({@code application/problem+json}) with HTTP 401 when an unauthenticated request
 * reaches a protected resource.
 *
 * <p>Registered in {@link com.company.expensetracker.config.SecurityConfig} as the
 * {@code authenticationEntryPoint} for the security filter chain.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Writes a 401 Unauthorized Problem Details body to the response.
     *
     * @param request        the request that triggered the authentication failure
     * @param response       the response to write the 401 body into
     * @param authException  the Spring Security exception that initiated this entry point
     * @throws IOException if writing to the response stream fails
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Authentication required");
        problem.setType(URI.create("https://httpstatuses.io/401"));
        problem.setTitle("Unauthorized");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
