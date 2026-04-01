package com.cts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayHeaderFilter(),
                        AbstractPreAuthenticatedProcessingFilter.class);

        return http.build();
    }

    @Bean
    public Filter gatewayHeaderFilter() {
        return (ServletRequest req, ServletResponse res, FilterChain chain) -> {
            HttpServletRequest request = (HttpServletRequest) req;

            String userId = request.getHeader("X-User-Id");
            String role = request.getHeader("X-User-Role");
            String email = request.getHeader("X-User-Email");

            // ADD THESE DEBUG LINES
            System.out.println("X-User-Id: " + userId);
            System.out.println("X-User-Role: " + role);
            System.out.println("X-User-Email: " + email);

            if (userId != null && role != null) {
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + role);

                org.springframework.security.authentication
                        .UsernamePasswordAuthenticationToken authToken =
                        new org.springframework.security.authentication
                                .UsernamePasswordAuthenticationToken(
                                email, null, List.of(authority));

                org.springframework.security.core.context
                        .SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }

            chain.doFilter(req, res);
        };
    }
}