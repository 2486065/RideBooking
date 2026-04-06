package com.cts;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class RideBookingServiceApplication {

    public static void main(String[] args) {

        // Load .env file and set as system properties BEFORE Spring starts
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()  // Won't crash if .env is absent (e.g., in production)
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(RideBookingServiceApplication.class, args);
    }
}