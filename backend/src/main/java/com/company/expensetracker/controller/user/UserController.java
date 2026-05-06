package com.company.expensetracker.controller.user;

import com.company.expensetracker.dto.user.ChangePasswordRequest;
import com.company.expensetracker.dto.user.RegisterRequest;
import com.company.expensetracker.dto.user.UserResponse;
import com.company.expensetracker.security.UserPrincipal;
import com.company.expensetracker.service.user.UserCommandService;
import com.company.expensetracker.service.user.UserQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UserController(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userCommandService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = userQueryService.findMe(principal.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userCommandService.changePassword(principal.userId(), request);
        return ResponseEntity.noContent().build();
    }
}
