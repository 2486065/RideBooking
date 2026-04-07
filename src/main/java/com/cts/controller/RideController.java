package com.cts.controller;

import com.cts.dto.*;
import com.cts.exception.ErrorResponse;
import com.cts.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/rides")
@Tag(name = "Ride Booking", description = "APIs for managing ride bookings")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @Operation(summary = "Book a ride", description = "Customer books a new ride")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride booked successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - CUSTOMER role required")
    })
    @PostMapping("/book")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RideResponse> bookRide(
            @Valid @RequestBody BookRideRequest request,
            @RequestHeader("Authorization") String token,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("POST /api/rides/book - User: {}", email);
        RideResponse response = rideService.bookRide(request, email, token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Accept a ride", description = "Driver accepts an available ride")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride accepted"),
            @ApiResponse(responseCode = "400", description = "Ride not in REQUESTED status"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @PutMapping("/accept/{rideId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> acceptRide(
            @PathVariable Long rideId,
            @RequestHeader("Authorization") String token,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("PUT /api/rides/accept/{} - Driver: {}", rideId, email);
        RideResponse response = rideService.acceptRide(rideId, email, token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Assign driver to ride", description = "Admin assigns a driver to a ride")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver assigned"),
            @ApiResponse(responseCode = "400", description = "Invalid request or ride state"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @PutMapping("/assign/{rideId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RideResponse> assignDriver(
            @PathVariable Long rideId,
            @Valid @RequestBody AssignDriverRequest request,
            @RequestHeader("Authorization") String token) {
        log.info("PUT /api/rides/assign/{} - Assigning driver: {}", rideId, request.getDriverId());
        RideResponse response = rideService.assignDriver(rideId, request, token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update ride status", description = "Driver updates ride to IN_PROGRESS or COMPLETED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Not assigned to this ride")
    })
    @PutMapping("/status/{rideId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> updateRideStatus(
            @PathVariable Long rideId,
            @Valid @RequestBody UpdateRideStatusRequest request,
            @RequestHeader("Authorization") String token,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("PUT /api/rides/status/{} - Driver: {} - NewStatus: {}",
                rideId, email, request.getStatus());
        RideResponse response = rideService.updateRideStatus(rideId, request, email, token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel a ride", description = "Customer cancels their own ride")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride cancelled"),
            @ApiResponse(responseCode = "400", description = "Ride already completed/cancelled"),
            @ApiResponse(responseCode = "403", description = "Can only cancel own rides")
    })
    @PutMapping("/cancel/{rideId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable Long rideId,
            @RequestHeader("Authorization") String token,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("PUT /api/rides/cancel/{} - User: {}", rideId, email);
        RideResponse response = rideService.cancelRide(rideId, email, token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get my rides (Customer)", description = "Customer views all their booked rides")
    @GetMapping("/my-rides")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<RideResponse>> getMyRides(
            @RequestHeader("Authorization") String token,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("GET /api/rides/my-rides - Customer: {}", email);
        List<RideResponse> rides = rideService.getMyRides(email, token);
        return ResponseEntity.ok(rides);
    }

    @Operation(summary = "Get my rides (Driver)", description = "Driver views all their assigned rides")
    @GetMapping("/my-rides/driver")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<RideResponse>> getMyDriverRides(
            @RequestHeader("Authorization") String token,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("GET /api/rides/my-rides/driver - Driver: {}", email);
        List<RideResponse> rides = rideService.getMyDriverRides(email, token);
        return ResponseEntity.ok(rides);
    }

    @Operation(summary = "Get ride by ID", description = "Any authenticated user can view a ride")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride found"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRideById(@PathVariable Long rideId) {
        log.info("GET /api/rides/{}", rideId);
        RideResponse response = rideService.getRideById(rideId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get rides by user", description = "Get all rides for a customer")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<RideResponse>> getRidesByUser(@PathVariable Long userId) {
        log.info("GET /api/rides/user/{}", userId);
        List<RideResponse> rides = rideService.getRidesByUser(userId);
        return ResponseEntity.ok(rides);
    }

    @Operation(summary = "Get rides by driver", description = "Get all rides assigned to a driver")
    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<List<RideResponse>> getRidesByDriver(@PathVariable Long driverId) {
        log.info("GET /api/rides/driver/{}", driverId);
        List<RideResponse> rides = rideService.getRidesByDriver(driverId);
        return ResponseEntity.ok(rides);
    }

    @Operation(summary = "Get available rides", description = "Driver views all rides waiting for acceptance")
    @GetMapping("/available")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<RideResponse>> getAvailableRides() {
        log.info("GET /api/rides/available");
        List<RideResponse> rides = rideService.getAvailableRides();
        return ResponseEntity.ok(rides);
    }
}