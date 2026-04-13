package com.cts.feign;

import com.cts.dto.UserResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AuthServiceClientFallbackFactory implements FallbackFactory<AuthServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClientFallbackFactory.class);

    @Override
    public AuthServiceClient create(Throwable cause) {
        return new AuthServiceClient() {
            @Override
            public UserResponse getUserById(Long userId, String token) {
                log.error("Circuit breaker/Timeout triggered for getUserById. User ID: {}. Reason: {}",
                        userId, cause.getMessage());
                // Return a default response, cached data, or throw a custom exception
                return new UserResponse();
            }

            @Override
            public UserResponse getUserByEmail(String email, String token) {
                log.error("Circuit breaker/Timeout triggered for getUserByEmail. Email: {}. Reason: {}",
                        email, cause.getMessage());
                return new UserResponse();
            }
        };
    }
}