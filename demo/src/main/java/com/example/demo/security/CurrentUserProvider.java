package com.example.demo.security;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CurrentUserProvider {

    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID userId)) {
            throw new InsufficientAuthenticationException("No authenticated user in context");
        }
        return userId;
    }

    public String getCurrentAccessToken() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) {
            throw new InsufficientAuthenticationException("No request context available");
        }

        Object token = attrs.getRequest().getAttribute("ACCESS_TOKEN");

        if (!(token instanceof String accessToken) || accessToken.isBlank()) {
            throw new InsufficientAuthenticationException("No access token available");
        }

        return accessToken;
    }

    public boolean hasAnyRole(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Set<String> wanted = Arrays.stream(roles).map(r -> "ROLE_" + r).collect(Collectors.toSet());
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(wanted::contains);
    }
}