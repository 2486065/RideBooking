package com.cts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class FareCalculationService {

    private static final BigDecimal BASE_FARE = new BigDecimal("50.00");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("12.00");

    // A Simulated Fare
    public BigDecimal calculateFare(String pickup, String dropoff) {
        int simulatedKm = Math.abs(pickup.hashCode() - dropoff.hashCode()) % 30 + 3;
        BigDecimal distance = new BigDecimal(simulatedKm);
        BigDecimal fare = BASE_FARE.add(PER_KM_RATE.multiply(distance)).setScale(2, RoundingMode.HALF_UP);

        log.info("Fare calculated: {} for distance: {} km (from: '{}' to: '{}')",
                fare, simulatedKm, pickup, dropoff);

        return fare;
    }
}