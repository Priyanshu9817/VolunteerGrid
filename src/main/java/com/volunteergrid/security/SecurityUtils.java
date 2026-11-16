package com.volunteergrid.security;

import com.volunteergrid.entity.User;
import com.volunteergrid.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility component to retrieve the currently authenticated user
 * from the Spring Security context.
 *
 * Injected into services to get "who is making this request."
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the currently logged-in User entity.
     * Throws IllegalStateException if no authenticated user exists.
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            throw new IllegalStateException("No authenticated user in context");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in DB: " + email));
    }

    /**
     * Returns the current user's email (principal name) without a DB query.
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
