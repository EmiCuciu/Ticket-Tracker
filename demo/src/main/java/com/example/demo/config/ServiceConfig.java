package com.example.demo.config;

import com.example.demo.mapper.CommentMapper;
import com.example.demo.mapper.JsonNullableMapper;
import com.example.demo.mapper.ProjectMapper;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.repository.*;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.security.JwtService;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.implementation.*;
import com.example.demo.web.TicketPageableResolver;
import com.example.demo.web.TicketSearchValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ServiceConfig {

    @Bean
    public TicketServiceImpl ticketService(TicketRepository ticketRepository,
                                           ProjectRepository projectRepository,
                                           UserRepository userRepository,
                                           TicketMapper ticketMapper,
                                           TicketSearchValidator searchValidator,
                                           TicketPageableResolver pageableResolver,
                                           JsonNullableMapper jsonNullableMapper,
                                           CurrentUserProvider currentUserProvider) {
        return new TicketServiceImpl(ticketRepository, projectRepository, userRepository,
                ticketMapper, searchValidator, pageableResolver, jsonNullableMapper, currentUserProvider);
    }

    @Bean
    public ProjectServiceImpl projectService(ProjectRepository projectRepository,
                                             UserRepository userRepository,
                                             ProjectMapper projectMapper,
                                             CurrentUserProvider currentUserProvider) {
        return new ProjectServiceImpl(projectRepository, userRepository, projectMapper, currentUserProvider);
    }

    @Bean
    public CommentServiceImpl commentService(CommentRepository commentRepository,
                                             TicketRepository ticketRepository,
                                             UserRepository userRepository,
                                             CommentMapper commentMapper,
                                             CurrentUserProvider currentUserProvider) {
        return new CommentServiceImpl(commentRepository, ticketRepository, userRepository, commentMapper, currentUserProvider);
    }

    @Bean
    public AuthServiceImpl authService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                       AuthenticationManager authenticationManager, JwtService jwtService,
                                       RefreshTokenService refreshTokenService, TokenBlacklistServiceImpl tokenBlacklistService) {
        return new AuthServiceImpl(userRepository, passwordEncoder, authenticationManager, jwtService, refreshTokenService, tokenBlacklistService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsServiceImpl userDetailsService(UserRepository userRepository) {
        return new UserDetailsServiceImpl(userRepository);
    }

    @Bean
    public RefreshTokenServiceImpl refreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        return new RefreshTokenServiceImpl(refreshTokenRepository);
    }

    @Bean
    public TokenBlacklistServiceImpl tokenBlacklistService(TokenBlacklistRepository tokenBlacklistRepository) {
        return new TokenBlacklistServiceImpl(tokenBlacklistRepository);
    }
}