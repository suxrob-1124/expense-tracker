package com.company.expensetracker.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Spring Security {@link AccessDeniedHandler} that returns an RFC 7807 Problem Details
 * response ({@code application/problem+json}) with HTTP 403 when an authenticated principal
 * lacks the required authority for the requested resource.
 *
 * <p>Registered in {@link com.company.expensetracker.config.SecurityConfig} as the
 * {@code accessDeniedHandler} for the security filter chain.
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Writes a 403 Forbidden Problem Details body to the response.
     *
     * @param request                 the request that triggered the access-denied decision
     * @param response                the response to write the 403 body into
     * @param accessDeniedException   the Spring Security exception describing the denial
     * @throws IOException if writing to the response stream fails
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Access denied");
        problem.setType(URI.create("https://httpstatuses.io/403"));
        problem.setTitle("Forbidden");

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
