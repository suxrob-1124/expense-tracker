package com.company.expensetracker.domain;

import com.company.expensetracker.crypto.AesGcmStringConverter;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "email_encrypted", nullable = false)
    private String email;

    @Column(name = "email_hash", nullable = false, unique = true, length = 64)
    private String emailHash;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "first_name_encrypted")
    private String firstName;

    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "last_name_encrypted")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {}

    public User(String email, String emailHash, String passwordHash, Role role) {
        this.email = email;
        this.emailHash = emailHash;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void lockUntil(Instant until) {
        this.lockedUntil = until;
    }

    public void updateLastLoginAt(Instant at) {
        this.lastLoginAt = at;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getEmail() { return email; }
    public String getEmailHash() { return emailHash; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getLastLoginAt() { return lastLoginAt; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
