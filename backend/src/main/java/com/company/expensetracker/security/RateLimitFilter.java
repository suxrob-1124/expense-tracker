package com.company.expensetracker.security;

import io.github.bucket4j.Bucket;
import com.company.expensetracker.config.RateLimitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

/**
 * Servlet filter that enforces per-IP rate limiting using Bucket4j.
 *
 * <p>Auth paths ({@code /api/v1/auth/**}) are limited to 10 requests per minute;
 * all other paths to 60 requests per minute. Exceeding either limit returns
 * {@code 429 Too Many Requests} with an RFC 7807 Problem Details body
 * ({@code application/problem+json}).
 *
 * <p>The client IP is resolved from the {@code X-Forwarded-For} header (first value)
 * when present, otherwise from the remote address.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
        this.rateLimitConfig = rateLimitConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Selects the appropriate Bucket4j bucket for the request's IP and path,
     * then either forwards the request or returns a 429 response.
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
        String ip = resolveClientIp(request);
        boolean isAuthPath = request.getRequestURI().contains("/api/v1/auth");

        Bucket bucket = isAuthPath
                ? rateLimitConfig.resolveAuthBucket(ip)
                : rateLimitConfig.resolveDefaultBucket(ip);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        problem.setType(URI.create("https://httpstatuses.io/429"));
        problem.setTitle("Too Many Requests");

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getWriter(), problem);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
