package com.example.demo.unit.security;

import com.example.demo.config.SecurityPathsProperties;
import com.example.demo.domain.Role;
import com.example.demo.security.JwtAuthenticationEntryPoint;
import com.example.demo.security.RoleAuthorizationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Profile("test")
class RoleAuthorizationFilterTest {

    private RoleAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityPathsProperties props = new SecurityPathsProperties(
                List.of(),
                Map.of(Role.ADMIN, List.of("DELETE:/api/projects/**"))
        );

        filter = new RoleAuthorizationFilter(
                props,
                new JwtAuthenticationEntryPoint(
                        new ObjectMapper()),
                new ObjectMapper());
    }

    @Test
    void memberHittingAdminEndpoint_returns403() throws Exception {
        SecurityContextHolder.getContext().
                setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                UUID.randomUUID(),
                                null,
                                List.of(
                                        new SimpleGrantedAuthority("ROLE_MEMBER")
                                )));

        MockHttpServletRequest request = new MockHttpServletRequest(
                "DELETE", "/api/projects/{id}");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void adminHittingAdminEndpoint_isAllowed() throws Exception {
        SecurityContextHolder.getContext().
                setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                UUID.randomUUID(),
                                null,
                                List.of(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                )));

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/projects/1234");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        doAnswer(invocation -> {
                    HttpServletResponse response1 = invocation.getArgument(1);
                    response1.setStatus(204);
                    return null;
                }
        ).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(204);
        verify(chain).doFilter(request, response);
    }
}