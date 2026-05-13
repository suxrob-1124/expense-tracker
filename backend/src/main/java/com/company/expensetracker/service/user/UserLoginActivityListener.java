package com.company.expensetracker.service.user;

import com.company.expensetracker.domain.User;
import com.company.expensetracker.event.UserLoggedInEvent;
import com.company.expensetracker.event.UserLoginFailedEvent;
import com.company.expensetracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Spring event listener that updates login-related state on the {@link com.company.expensetracker.domain.User} entity.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@link com.company.expensetracker.event.UserLoggedInEvent} — resets failed-attempt counter and updates {@code lastLoginAt}.</li>
 *   <li>{@link com.company.expensetracker.event.UserLoginFailedEvent} — increments {@code failedLoginAttempts} and locks
 *       the account for {@value #LOCKOUT_MINUTES} minutes after {@value #LOCKOUT_THRESHOLD} consecutive failures.</li>
 * </ul>
 *
 * <p>Both handlers are {@code @Async} to avoid blocking the authentication thread.
 */
@Component
public class UserLoginActivityListener {

    private static final Logger log = LoggerFactory.getLogger(UserLoginActivityListener.class);
    private static final int LOCKOUT_THRESHOLD = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;

    public UserLoginActivityListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resets the failed-login counter and records the login timestamp.
     *
     * @param event the login event carrying userId and timestamp
     */
    @EventListener
    @Async
    @Transactional
    public void onUserLoggedIn(UserLoggedInEvent event) {
        userRepository.findById(event.userId()).ifPresent(user -> {
            user.resetFailedAttempts();
            user.updateLastLoginAt(event.occurredAt());
        });
    }

    /**
     * Increments the failed-login counter and locks the account after the threshold is reached.
     *
     * @param event the failed-login event carrying the email hash and reason
     */
    @EventListener
    @Async
    @Transactional
    public void onUserLoginFailed(UserLoginFailedEvent event) {
        userRepository.findByEmailHash(event.emailHash()).ifPresent(user -> {
            user.incrementFailedAttempts();
            if (user.getFailedLoginAttempts() >= LOCKOUT_THRESHOLD) {
                user.lockUntil(Instant.now().plusSeconds(LOCKOUT_MINUTES * 60L));
                log.warn("Account locked for emailHash={} after {} failed attempts",
                        event.emailHash(), user.getFailedLoginAttempts());
            }
        });
    }
}
