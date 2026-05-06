package com.company.expensetracker.security;

import com.company.expensetracker.crypto.EmailHasher;
import com.company.expensetracker.domain.User;
import com.company.expensetracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmailHasher emailHasher;

    public CustomUserDetailsService(UserRepository userRepository, EmailHasher emailHasher) {
        this.userRepository = userRepository;
        this.emailHasher = emailHasher;
    }

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
