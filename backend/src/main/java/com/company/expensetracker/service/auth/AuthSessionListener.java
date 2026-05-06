package com.company.expensetracker.service.auth;

import com.company.expensetracker.event.PasswordChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionListener {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionListener.class);

    // Placeholder for future jti-blacklist invalidation.
    // When a jti-blacklist store is added, load all active refresh tokens for userId
    // and add their jti claims to the blacklist here.
    @EventListener
    public void onPasswordChanged(PasswordChangedEvent event) {
        log.info("Password changed for userId={}; refresh token invalidation pending jti-blacklist implementation",
                event.userId());
    }
}
