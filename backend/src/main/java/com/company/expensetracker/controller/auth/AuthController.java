package com.company.expensetracker.controller.auth;

import com.company.expensetracker.dto.auth.AuthResponse;
import com.company.expensetracker.dto.auth.LoginRequest;
import com.company.expensetracker.dto.user.UserResponse;
import com.company.expensetracker.service.auth.AuthService;
import com.company.expensetracker.service.auth.LoginTokens;
import com.company.expensetracker.service.user.UserQueryService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final UserQueryService userQueryService;

    public AuthController(AuthService authService, UserQueryService userQueryService) {
        this.authService = authService;
        this.userQueryService = userQueryService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginTokens tokens = authService.login(request, httpRequest);
        setRefreshCookie(httpResponse, tokens.refreshToken(), (int) tokens.refreshTtlSeconds());
        UserResponse userResponse = userQueryService.findById(tokens.userId());
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), "Bearer", tokens.accessTtlSeconds(), userResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = extractRefreshCookie(httpRequest);
        LoginTokens tokens = authService.refresh(refreshToken);
        setRefreshCookie(httpResponse, tokens.refreshToken(), (int) tokens.refreshTtlSeconds());
        UserResponse userResponse = userQueryService.findById(tokens.userId());
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), "Bearer", tokens.accessTtlSeconds(), userResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
