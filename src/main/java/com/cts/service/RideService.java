package com.cts.service;

import com.cts.dto.*;
import com.cts.entity.Ride;
import com.cts.entity.RideStatus;
import com.cts.exception.InvalidRideStateException;
import com.cts.exception.RideNotFoundException;
import com.cts.exception.UnauthorizedAccessException;
import com.cts.feign.AuthServiceClient;
import com.cts.repository.RideRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RideService {

    private final RideRepository rideRepository;
    private final FareCalculationService fareService;
    private final AuthServiceClient authServiceClient;

    public RideService(RideRepository rideRepository,
                       FareCalculationService fareService,
                       AuthServiceClient authServiceClient) {
        this.rideRepository = rideRepository;
        this.fareService = fareService;
        this.authServiceClient = authServiceClient;
    }

    @Transactional
    public RideResponse bookRide(BookRideRequest request, String email, String token) {
        log.info("Booking ride for user: {} from '{}' to '{}'",
                email, request.getPickupLocation(), request.getDropoffLocation());

        UserResponse user = authServiceClient.getUserByEmail(email, token);
        log.debug("Fetched user details from Auth Service: userId={}", user.getUserId());

        Ride ride = Ride.builder()
                .userId(user.getUserId())
                .pickupLocation(request.getPickupLocation())
                .dropoffLocation(request.getDropoffLocation())
                .fare(fareService.calculateFare(request.getPickupLocation(), request.getDropoffLocation()))
                .status(RideStatus.REQUESTED)
                .build();

        Ride saved = rideRepository.save(ride);
        log.info("Ride booked successfully: rideId={}, userId={}, fare={}",
                saved.getRideId(), saved.getUserId(), saved.getFare());

        return mapToResponse(saved);
    }

    @Transactional
    public RideResponse assignDriver(Long rideId, AssignDriverRequest request, String token) {
        log.info("Admin assigning driver {} to ride {}", request.getDriverId(), rideId);

        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStateException(
                    "Driver can only be assigned to rides in REQUESTED status");
        }

        UserResponse driver = authServiceClient.getUserById(request.getDriverId(), token);
        if (!"DRIVER".equals(driver.getRole().name())) {
            throw new InvalidRideStateException(
                    "User " + request.getDriverId() + " is not a DRIVER");
        }

        // Check if driver already has active rides
        long activeRides = rideRepository.countActiveRidesByDriver(request.getDriverId());
        if (activeRides > 0) {
            throw new InvalidRideStateException(
                    "Driver " + request.getDriverId() + " already has an active ride");
        }

        ride.setDriverId(request.getDriverId());
        ride.setStatus(RideStatus.ACCEPTED);
        Ride updated = rideRepository.save(ride);

        log.info("Driver {} assigned to ride {} successfully", request.getDriverId(), rideId);
        return mapToResponse(updated);
    }

    @Transactional
    public RideResponse acceptRide(Long rideId, String email, String token) {
        log.info("Driver {} accepting ride {}", email, rideId);

        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStateException("Only REQUESTED rides can be accepted");
        }

        UserResponse driver = authServiceClient.getUserByEmail(email, token);

        // Check if driver already has active rides
        long activeRides = rideRepository.countActiveRidesByDriver(driver.getUserId());
        if (activeRides > 0) {
            throw new InvalidRideStateException("You already have an active ride");
        }

        ride.setDriverId(driver.getUserId());
        ride.setStatus(RideStatus.ACCEPTED);
        Ride updated = rideRepository.save(ride);

        log.info("Ride {} accepted by driver {} (userId={})", rideId, email, driver.getUserId());
        return mapToResponse(updated);
    }

    @Transactional
    public RideResponse updateRideStatus(Long rideId, UpdateRideStatusRequest request,
                                         String email, String token) {
        log.info("Driver {} updating ride {} status to {}", email, rideId, request.getStatus());

        Ride ride = findRideOrThrow(rideId);

        UserResponse driver = authServiceClient.getUserByEmail(email, token);
        if (!ride.getDriverId().equals(driver.getUserId())) {
            throw new UnauthorizedAccessException("You are not assigned to this ride");
        }

        validateStatusTransition(ride.getStatus(), request.getStatus());
        ride.setStatus(request.getStatus());
        Ride updated = rideRepository.save(ride);

        log.info("Ride {} status updated to {} by driver {}", rideId, request.getStatus(), email);
        return mapToResponse(updated);
    }

    @Transactional
    public RideResponse cancelRide(Long rideId, String email, String token) {
        log.info("User {} cancelling ride {}", email, rideId);

        Ride ride = findRideOrThrow(rideId);

        UserResponse user = authServiceClient.getUserByEmail(email, token);
        if (!ride.getUserId().equals(user.getUserId())) {
            throw new UnauthorizedAccessException("You can only cancel your own rides");
        }

        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideStateException(
                    "Cannot cancel a ride that is already " + ride.getStatus());
        }

        ride.setStatus(RideStatus.CANCELLED);
        Ride updated = rideRepository.save(ride);

        log.info("Ride {} cancelled by user {}", rideId, email);
        return mapToResponse(updated);
    }

    public RideResponse getRideById(Long rideId) {
        log.debug("Fetching ride by id: {}", rideId);
        Ride ride = findRideOrThrow(rideId);
        return mapToResponse(ride);
    }

    public List<RideResponse> getRidesByUser(Long userId) {
        log.debug("Fetching rides for user: {}", userId);
        return rideRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getRidesByDriver(Long driverId) {
        log.debug("Fetching rides for driver: {}", driverId);
        return rideRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getAvailableRides() {
        log.debug("Fetching all available rides");
        return rideRepository.findByStatus(RideStatus.REQUESTED)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- Get my rides (CUSTOMER - no userId needed) ---
    public List<RideResponse> getMyRides(String email, String token) {
        log.debug("Fetching rides for customer: {}", email);
        UserResponse user = authServiceClient.getUserByEmail(email, token);
        return rideRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- Get my rides (DRIVER - no driverId needed) ---
    public List<RideResponse> getMyDriverRides(String email, String token) {
        log.debug("Fetching rides for driver: {}", email);
        UserResponse driver = authServiceClient.getUserByEmail(email, token);
        return rideRepository.findByDriverIdOrderByCreatedAtDesc(driver.getUserId())
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- Private Helpers ---

    private Ride findRideOrThrow(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found with id: " + rideId));
    }

    private void validateStatusTransition(RideStatus current, RideStatus next) {
        boolean valid = switch (current) {
            case REQUESTED -> next == RideStatus.ACCEPTED || next == RideStatus.CANCELLED;
            case ACCEPTED -> next == RideStatus.IN_PROGRESS || next == RideStatus.CANCELLED;
            case IN_PROGRESS -> next == RideStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidRideStateException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

    private RideResponse mapToResponse(Ride ride) {
        return RideResponse.builder()
                .rideId(ride.getRideId())
                .userId(ride.getUserId())
                .driverId(ride.getDriverId())
                .pickupLocation(ride.getPickupLocation())
                .dropoffLocation(ride.getDropoffLocation())
                .fare(ride.getFare())
                .status(ride.getStatus())
                .createdAt(ride.getCreatedAt())
                .updatedAt(ride.getUpdatedAt())
                .build();
    }
}