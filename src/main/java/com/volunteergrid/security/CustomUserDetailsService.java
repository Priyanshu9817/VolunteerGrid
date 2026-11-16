package com.volunteergrid.security;

import com.volunteergrid.entity.User;
import com.volunteergrid.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Adapter between our User entity and Spring Security's UserDetails.
 *
 * Why a separate class?
 * The User entity is a pure JPA/data class.  This adapter adds the
 * security contract (loadUserByUsername) without polluting the entity.
 *
 * Spring Security calls this during authentication to load user details.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));

        // Role stored as "ROLE_ADMIN", "ROLE_ORGANIZER", or "ROLE_VOLUNTEER"
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(authority))
                .accountLocked(!user.isEnabled())
                .disabled(!user.isEnabled())
                .build();
    }
}
