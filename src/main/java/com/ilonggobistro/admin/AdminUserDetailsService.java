package com.ilonggobistro.admin;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository repository;

    public AdminUserDetailsService(AdminUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String clean = identifier != null ? identifier.trim() : "";
        AdminUser admin = repository.findByUsernameIgnoreCase(clean)
                .or(() -> repository.findByEmailIgnoreCase(clean))
                .orElseThrow(() -> new UsernameNotFoundException("Staff account not found: " + identifier));

        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPasswordHash())
                .roles(admin.getRole())
                .disabled(!admin.isEnabled())
                .build();
    }
}
