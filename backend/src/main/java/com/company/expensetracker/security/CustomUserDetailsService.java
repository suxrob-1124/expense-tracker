package com.company.expensetracker.security;

import com.company.expensetracker.crypto.EmailHasher;
import com.company.expensetracker.domain.User;
import com.company.expensetracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security {@link UserDetailsService} implementation that loads users by email.
 *
 * <p>The raw email is never stored or queried directly — it is hashed via
 * {@link com.company.expensetracker.crypto.EmailHasher} (SHA-256) before the database
 * lookup, matching the stored {@code email_hash} column.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmailHasher emailHasher;

    public CustomUserDetailsService(UserRepository userRepository, EmailHasher emailHasher) {
        this.userRepository = userRepository;
        this.emailHasher = emailHasher;
    }

    /**
     * Loads a {@link UserPrincipal} by the user's raw email address.
     *
     * <p>The email is hashed with SHA-256 before lookup. Called by Spring Security's
     * {@code AuthenticationManager} during credential validation.
     *
     * @param email the plain email address supplied by the login request
     * @return the corresponding {@link UserPrincipal}
     * @throws UsernameNotFoundException if no user with that email hash exists
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String emailHash = emailHasher.hash(email);
        User user = userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserPrincipal(
                user.getId(),
                user.getEmailHash(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.isEnabled(),
                user.getLockedUntil()
        );
    }
}
