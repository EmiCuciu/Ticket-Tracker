package com.example.demo.security;

import com.example.demo.config.SecurityPathsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RoleAuthorizationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final List<String> pathsToSkip;
    private final Map<String, List<Rule>> rulesByRole;
    private final AuthenticationEntryPoint entryPoint;
    private final ObjectMapper objectMapper;

    public RoleAuthorizationFilter(SecurityPathsProperties props,
                                   AuthenticationEntryPoint entryPoint,
                                   ObjectMapper objectMapper) {
        this.pathsToSkip = props.pathsToSkip();
        this.rulesByRole = props.roleEndpoints().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> e.getValue().stream().map(Rule::parse).toList()));
        this.entryPoint = entryPoint;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || pathsToSkip.stream().anyMatch(p -> MATCHER.match(p.trim(), uri));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            entryPoint.commence(request, response,
                    new InsufficientAuthenticationException("Authentication required"));
            return;
        }

        boolean allowed = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .anyMatch(role -> rulesByRole.getOrDefault(role, List.of()).stream()
                        .anyMatch(rule -> rule.matches(request)));

        if (!allowed) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", 403,
                    "error", "Forbidden",
                    "message", "Access denied",
                    "path", request.getRequestURI())));
            return;
        }

        chain.doFilter(request, response);
    }

    private record Rule(String method, String pattern) {
        static Rule parse(String raw) {
            int i = raw.indexOf(':');
            return new Rule(raw.substring(0, i).trim(), raw.substring(i + 1).trim());
        }

        boolean matches(HttpServletRequest req) {
            return ("*".equals(method) || method.equalsIgnoreCase(req.getMethod()))
                    && MATCHER.match(pattern, req.getRequestURI());
        }
    }
}