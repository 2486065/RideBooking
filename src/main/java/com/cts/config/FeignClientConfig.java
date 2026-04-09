package com.cts.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate template) -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                String userId = request.getHeader("X-User-Id");
                String role = request.getHeader("X-User-Role");
                String email = request.getHeader("X-User-Email");

                if (userId != null) template.header("X-User-Id", userId);
                if (role != null) template.header("X-User-Role", role);
                if (email != null) template.header("X-User-Email", email);

                log.debug("Forwarding gateway headers to Feign request: {}", template.url());
            }
        };
    }
}