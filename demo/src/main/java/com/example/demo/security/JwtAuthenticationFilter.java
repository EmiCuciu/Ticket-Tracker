package com.example.demo.security;

import com.example.demo.service.implementation.UserDetailsServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.flywaydb.core.internal.util.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/auth/register", "/api/auth/login", "/api/auth/refresh"
    );
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;

    public JwtAuthenticationFilter(UserDetailsServiceImpl userDetailsService, JwtService jwtService) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        if (PUBLIC_AUTH_PATHS.contains(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        UUID userId = null;
        String jwt = null;

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            jwt = header.substring(7);
            try {
                userId = jwtService.extractUserId(jwt);
            } catch (ExpiredJwtException e) {
                logger.warn("Expired JWT: " + e.getMessage());
            } catch (JwtException | IllegalArgumentException e) {
                logger.warn("Invalid JWT: " + e.getMessage());
            }
        }

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            if (jwtService.validateToken(jwt, userId)) {
                request.setAttribute("ACCESS_TOKEN", jwt);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userId, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
