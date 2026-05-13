package com.company.expensetracker.controller.user;

import com.company.expensetracker.dto.user.ChangePasswordRequest;
import com.company.expensetracker.dto.user.RegisterRequest;
import com.company.expensetracker.dto.user.UserResponse;
import com.company.expensetracker.security.UserPrincipal;
import com.company.expensetracker.service.user.UserCommandService;
import com.company.expensetracker.service.user.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user account management.
 *
 * <p>Base path: {@code /api/v1/users}. {@code POST /register} is publicly accessible;
 * {@code GET /me} and {@code POST /me/password} require a valid Bearer token.
 *
 * <p>Write operations are delegated to {@link UserCommandService};
 * read operations are delegated to {@link UserQueryService}.
 */
@Tag(name = "Users", description = "User registration, profile retrieval and password management.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UserController(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    /**
     * Creates a new user account.
     *
     * <p>Publicly accessible — no authentication required. The email is stored
     * AES-256-GCM encrypted; lookups use its SHA-256 hash.
     *
     * @param request registration payload containing email, password, firstName and lastName
     * @return {@code 201 Created} with the created {@link UserResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 409} if the email is already registered
     */
    @Operation(summary = "Register new user",
            description = "Creates a new user account. Email is stored encrypted; lookup uses SHA-256 hash.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Email already registered"),
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userCommandService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns the authenticated user's profile.
     *
     * @param principal the authenticated user, injected by Spring Security
     * @return {@code 200 OK} with the current {@link UserResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 401} if not authenticated
     */
    @Operation(summary = "Get current user profile",
            description = "Returns the profile of the currently authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = userQueryService.findMe(principal.userId());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the authenticated user's password.
     *
     * <p>Requires the current password for verification. Note: active sessions on
     * other devices are not invalidated until a full JTI-blacklist sweep is implemented.
     *
     * @param principal the authenticated user
     * @param request   contains the current password (for verification) and the new password
     * @return {@code 204 No Content}
     * @throws org.springframework.web.server.ResponseStatusException {@code 400} if the current password is wrong
     */
    @Operation(summary = "Change password",
            description = "Updates the user's password. Requires the current password for verification.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password updated"),
            @ApiResponse(responseCode = "400", description = "Current password is incorrect or validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
    })
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userCommandService.changePassword(principal.userId(), request);
        return ResponseEntity.noContent().build();
    }
}
