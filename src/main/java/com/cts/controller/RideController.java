package com.cts.controller;

import com.cts.dto.*;
import com.cts.entity.Role;
import com.cts.exception.UnauthorizedAccessException;
import com.cts.repository.RideRepository;
import com.cts.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    // --- CUSTOMER: Book a ride ---
    @PostMapping("/book")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RideResponse> bookRide(@RequestBody BookRideRequest request,
                                                 @RequestHeader("X-User-Id") Long userId) {
        RideResponse response = rideService.bookRide(request, userId);
        return ResponseEntity.ok(response);
    }

    // --- DRIVER: Accept a ride ---
    @PutMapping("/accept/{rideId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable Long rideId,
                                                   @RequestHeader("X-User-Id") Long driverId) {
        RideResponse response = rideService.acceptRide(rideId, driverId);
        return ResponseEntity.ok(response);
    }

    // --- ADMIN: Assign a driver to a ride ---
    @PutMapping("/assign/{rideId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RideResponse> assignDriver(@PathVariable Long rideId,
                                                     @RequestBody AssignDriverRequest request) {
        RideResponse response = rideService.assignDriver(rideId, request);
        return ResponseEntity.ok(response);
    }

    // --- DRIVER: Update ride status (IN_PROGRESS, COMPLETED) ---
    @PutMapping("/status/{rideId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> updateRideStatus(@PathVariable Long rideId,
                                                         @RequestBody UpdateRideStatusRequest request,
                                                         @RequestHeader("X-User-Id") Long driverId) {
        RideResponse response = rideService.updateRideStatus(rideId, request, driverId);
        return ResponseEntity.ok(response);
    }

    // --- CUSTOMER: Cancel a ride ---
    @PutMapping("/cancel/{rideId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable Long rideId,
                                                   @RequestHeader("X-User-Id") Long userId) {
        RideResponse response = rideService.cancelRide(rideId, userId);
        return ResponseEntity.ok(response);
    }

    // --- Get ride by ID (any authenticated user) ---
    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRideById(@PathVariable Long rideId) {
        RideResponse response = rideService.getRideById(rideId);
        return ResponseEntity.ok(response);
    }

    // --- CUSTOMER: Get my rides ---
    @GetMapping("/my-rides")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<RideResponse>> getRidesByUser(
            @RequestHeader("X-User-Id") Long userId) {
        List<RideResponse> rides = rideService.getRidesByUser(userId);
        return ResponseEntity.ok(rides);
    }

    // --- DRIVER: Get my assigned rides ---
    @GetMapping("/my-rides/driver")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<List<RideResponse>> getRidesByDriver(
            @RequestHeader("X-User-Id") Long driverId) {
        List<RideResponse> rides = rideService.getRidesByDriver(driverId);
        return ResponseEntity.ok(rides);
    }

    // --- DRIVER: Get all available rides to accept ---
    @GetMapping("/available")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<RideResponse>> getAvailableRides() {
        List<RideResponse> rides = rideService.getAvailableRides();
        return ResponseEntity.ok(rides);
    }
}

