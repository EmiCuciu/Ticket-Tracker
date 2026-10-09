package com.example.demo.config;

import com.example.demo.domain.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app.security")
public record SecurityPathsProperties(List<String> pathsToSkip,
                                      Map<Role, List<String>> roleEndpoints) {
    public SecurityPathsProperties {
        pathsToSkip = pathsToSkip == null ? List.of() : pathsToSkip;
        roleEndpoints = roleEndpoints == null ? Map.of() : roleEndpoints;
    }
}