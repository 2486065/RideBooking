package com.cts.feign;

import com.cts.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthServiceClient {

    @GetMapping("/api/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") Long userId,
                             @RequestHeader("Authorization") String token);

    @GetMapping("/api/users/email/{email}")
    UserResponse getUserByEmail(@PathVariable("email") String email,
                                @RequestHeader("Authorization") String token);
}