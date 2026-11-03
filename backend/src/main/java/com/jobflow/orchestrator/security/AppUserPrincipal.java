package com.jobflow.orchestrator.security;

import com.jobflow.orchestrator.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

public class AppUserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final String role;

    public AppUserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().name();
    }

    public UUID getId() { return id; }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    public String getRole() { return role; }
}
