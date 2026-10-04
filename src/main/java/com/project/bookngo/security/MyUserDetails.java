package com.project.bookngo.security;

import com.project.bookngo.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor

public class MyUserDetails implements UserDetails {
    @Getter
    private User user;

    @Override
    public boolean isEnabled() { // is user active
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() { // logged in
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
// SimpleGrantedAuthority only allows users to have a single role,, for eg: a user can only be an admin, a regular user, or an activity provider.
        return List.of(new SimpleGrantedAuthority("ROLE_"+ user.getRole().name()));
    }
}

