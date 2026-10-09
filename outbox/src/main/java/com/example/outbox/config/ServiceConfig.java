package com.example.outbox.config;

import com.example.outbox.service.impl.MailServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class ServiceConfig {

    @Bean
    public MailServiceImpl mailService(JavaMailSender javaMailSender) {
        return new MailServiceImpl(javaMailSender);
    }
}