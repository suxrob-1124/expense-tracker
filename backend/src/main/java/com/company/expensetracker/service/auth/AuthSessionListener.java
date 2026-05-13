package com.company.expensetracker.service.auth;

import com.company.expensetracker.event.PasswordChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spring event listener that reacts to session-affecting security events.
 *
 * <p>Handles {@link com.company.expensetracker.event.PasswordChangedEvent} by logging
 * a notice. Full refresh-token invalidation across all devices is deferred until a
 * per-user JTI index is available in the token blacklist store.
 */
@Component
public class AuthSessionListener {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionListener.class);

    /**
     * Called when a user changes their password.
     *
     * <p>Future enhancement: load all active refresh-token JTIs for the user
     * and add them to the blacklist, forcing re-authentication on all devices.
     *
     * @param event the password-changed event carrying the userId
     */
    // Placeholder for future jti-blacklist invalidation.
    // When a jti-blacklist store is added, load all active refresh tokens for userId
    // and add their jti claims to the blacklist here.
    @EventListener
    public void onPasswordChanged(PasswordChangedEvent event) {
        log.info("Password changed for userId={}; refresh token invalidation pending jti-blacklist implementation",
                event.userId());
    }
}
