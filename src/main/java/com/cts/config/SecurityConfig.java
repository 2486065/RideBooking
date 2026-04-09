package com.cts.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import java.util.List;

@Slf4j
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/actuator/**"
                        ).permitAll()
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

            log.debug("Incoming request - userId: {}, role: {}, email: {}", userId, role, email);

            if (userId != null && role != null) {
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                email, null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            chain.doFilter(req, res);
        };
    }
}