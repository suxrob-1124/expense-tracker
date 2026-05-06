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

@Component
public class UserLoginActivityListener {

    private static final Logger log = LoggerFactory.getLogger(UserLoginActivityListener.class);
    private static final int LOCKOUT_THRESHOLD = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;

    public UserLoginActivityListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener
    @Async
    @Transactional
    public void onUserLoggedIn(UserLoggedInEvent event) {
        userRepository.findById(event.userId()).ifPresent(user -> {
            user.resetFailedAttempts();
            user.updateLastLoginAt(event.occurredAt());
        });
    }

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
