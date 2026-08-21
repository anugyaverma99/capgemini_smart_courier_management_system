package com.CourierManagement.DeliveryService.Service;

import com.CourierManagement.DeliveryService.Client.AdminClient;
import com.CourierManagement.DeliveryService.Dto.*;
import com.CourierManagement.DeliveryService.Entity.*;
import com.CourierManagement.DeliveryService.Exception.DeliveryServiceException;
import com.CourierManagement.DeliveryService.Repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository repository;
    private final AdminClient adminClient;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    // create a new delivery
    public DeliveryResponse createDelivery(CreateDeliveryRequest request) {

        // build sender address
        Address sender = Address.builder()
                .name(request.getSenderAddress().getName())
                .email(request.getSenderAddress().getEmail())
                .phone(request.getSenderAddress().getPhone())
                .addressLine(request.getSenderAddress().getAddressLine())
                .city(request.getSenderAddress().getCity())
                .state(request.getSenderAddress().getState())
                .zipCode(request.getSenderAddress().getZipCode())
                .country(request.getSenderAddress().getCountry())
                .build();
        // build receiver address
        Address receiver = Address.builder()
                .name(request.getReceiverAddress().getName())
                .email(request.getReceiverAddress().getEmail())
                .phone(request.getReceiverAddress().getPhone())
                .addressLine(request.getReceiverAddress().getAddressLine())
                .city(request.getReceiverAddress().getCity())
                .state(request.getReceiverAddress().getState())
                .zipCode(request.getReceiverAddress().getZipCode())
                .country(request.getReceiverAddress().getCountry())
                .build();

        // build package details
        PackageDetails pkg = PackageDetails.builder()
                .description(request.getPackageDetails().getDescription())
                .weightKg(request.getPackageDetails().getWeightKg())
                .lengthCm(request.getPackageDetails().getLengthCm())
                .widthCm(request.getPackageDetails().getWidthCm())
                .heightCm(request.getPackageDetails().getHeightCm())
                .serviceType(request.getPackageDetails().getServiceType())
                .declaredValue(request.getPackageDetails().getDeclaredValue())
                .build();

        // calculate shipping charge based on weight and service type
        double charge = calculateCharge(pkg.getWeightKg(), pkg.getServiceType());

        // generate unique tracking number
        String trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // build the delivery entity
        Delivery delivery = Delivery.builder()
                .trackingNumber(trackingNumber)
                .customerId(request.getCustomerId())
                .senderAddress(sender)
                .receiverAddress(receiver)
                .packageDetails(pkg)
                .charge(charge)
                .pickupScheduledAt(request.getPickupScheduledAt())
                .status(DeliveryStatus.DRAFT)
                .build();

        // save delivery to database
        Delivery saved = repository.save(delivery);

        // notify admin service about new delivery
        try {
        	adminClient.syncDelivery(DeliveryMonitorRequest.builder()
                    .deliveryId(String.valueOf(saved.getId()))
                    .trackingNumber(saved.getTrackingNumber())
                    .customerName(request.getSenderAddress().getName())
                    .senderCity(request.getSenderAddress().getCity())
                    .receiverCity(request.getReceiverAddress().getCity())
                    .currentStatus(saved.getStatus().name())
                    .assignedHub("Unassigned")
                    .build());
        } catch (Exception e) {
            log.warn("Admin Service sync failed for delivery {}: {}", saved.getId(), e.getMessage());
        }

        // send order confirmation email via rabbitmq
        try {
            if (request.getReceiverAddress() != null && request.getReceiverAddress().getEmail() != null) {
                java.util.Map<String, Object> notificationEvent = java.util.Map.of(
                    "trackingNumber", saved.getTrackingNumber(),
                    "status", saved.getStatus().name() + " (Order Confirmed)",
                    "customerEmail", request.getReceiverAddress().getEmail(),
                    "customerName", request.getReceiverAddress().getName() != null ? request.getReceiverAddress().getName() : "Customer",
                    "remarks", "Your delivery has been successfully created and is being processed."
                );
                rabbitTemplate.convertAndSend(
                    com.CourierManagement.DeliveryService.Config.RabbitMQConfig.EXCHANGE_NAME, 
                    com.CourierManagement.DeliveryService.Config.RabbitMQConfig.ROUTING_KEY, 
                    notificationEvent
                );
            }
        } catch (Exception e) {
            log.warn("Failed to publish order confirmation email event: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    // update delivery lifecycle status
    public DeliveryResponse updateStatus(Long id, UpdateStatusRequest request) {

        // find the delivery
        Delivery delivery = repository.findById(id)
                .orElseThrow(() -> new DeliveryServiceException("Delivery not found: " + id));

        // check if this status transition is allowed
        validateStatusTransition(delivery.getStatus(), request.getStatus());

        // update and save
        delivery.setStatus(request.getStatus());
        Delivery saved = repository.save(delivery);

     
        try {
            adminClient.syncDelivery(DeliveryMonitorRequest.builder()
                    .deliveryId(String.valueOf(saved.getId()))
                    .trackingNumber(saved.getTrackingNumber())
                    .currentStatus(saved.getStatus().name())
                    .build());

           
            if (request.getStatus() == DeliveryStatus.DELAYED ||
            	    request.getStatus() == DeliveryStatus.FAILED ||
            	    request.getStatus() == DeliveryStatus.RETURNED) {

            	    adminClient.raiseException(ExceptionRequest.builder()
            	            .deliveryId(String.valueOf(saved.getId()))
            	            .trackingNumber(saved.getTrackingNumber())
            	            .exceptionStatus(request.getStatus().name()) 
            	            .reason("Delivery marked as " + request.getStatus().name()
            	                    + ". Remarks: " + request.getRemarks())
            	            .build());
            	}   
        } catch (Exception e) {
            log.warn("Admin Service notification failed for delivery {}: {}", saved.getId(), e.getMessage());
        }
      
        return toResponse(saved);
    }

    // get all deliveries for a customer
    public List<DeliveryResponse> getMyDeliveries(String customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    // find delivery by id
    public DeliveryResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new DeliveryServiceException("Delivery not found: " + id));
    }

    // check if delivery exists
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }
    // find delivery by tracking number
    public DeliveryResponse getByTrackingNumber(String trackingNumber) {
        return repository.findByTrackingNumber(trackingNumber)
                .map(this::toResponse)
                .orElseThrow(() -> new DeliveryServiceException(
                        "Delivery not found for tracking number: " + trackingNumber));
    }
    // get all deliveries (admin use)
    public List<DeliveryResponse> getAllDeliveries() {
        return repository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    // calculate shipping charge based on weight and service type
    private double calculateCharge(double weightKg, String serviceType) {
        double baseRate;
        switch (serviceType.toLowerCase()) {
            case "express": baseRate = 80.0; break;
            case "international": baseRate = 200.0; break;
            default: baseRate = 40.0; break;
        }
        return baseRate * Math.max(weightKg, 1.0);
    }

    // enforce valid delivery status transitions
    private void validateStatusTransition(DeliveryStatus current, DeliveryStatus next) {
        List<DeliveryStatus> lifecycle = List.of(
                DeliveryStatus.DRAFT,
                DeliveryStatus.BOOKED,
                DeliveryStatus.PICKED_UP,
                DeliveryStatus.IN_TRANSIT,
                DeliveryStatus.OUT_FOR_DELIVERY,
                DeliveryStatus.DELIVERED
        );

        List<DeliveryStatus> exceptionStates = List.of(
                DeliveryStatus.DELAYED,
                DeliveryStatus.FAILED,
                DeliveryStatus.RETURNED
        );

        if (exceptionStates.contains(next)) return;

        int currentIndex = lifecycle.indexOf(current);
        int nextIndex = lifecycle.indexOf(next);

        if (nextIndex <= currentIndex) {
            
            		throw new DeliveryServiceException(
            		        "Invalid status transition from " + current + " to " + next, 409);
        }
    }

    // convert entity to response dto
    private DeliveryResponse toResponse(Delivery d) {
        AddressDto senderDTO = AddressDto.builder()
                .name(d.getSenderAddress().getName())
                .email(d.getSenderAddress().getEmail())
                .phone(d.getSenderAddress().getPhone())
                .addressLine(d.getSenderAddress().getAddressLine())
                .city(d.getSenderAddress().getCity())
                .state(d.getSenderAddress().getState())
                .zipCode(d.getSenderAddress().getZipCode())
                .country(d.getSenderAddress().getCountry())
                .build();

        AddressDto receiverDTO = AddressDto.builder()
                .name(d.getReceiverAddress().getName())
                .email(d.getReceiverAddress().getEmail())
                .phone(d.getReceiverAddress().getPhone())
                .addressLine(d.getReceiverAddress().getAddressLine())
                .city(d.getReceiverAddress().getCity())
                .state(d.getReceiverAddress().getState())
                .zipCode(d.getReceiverAddress().getZipCode())
                .country(d.getReceiverAddress().getCountry())
                .build();

        PackageDto packageDTO = PackageDto.builder()
                .description(d.getPackageDetails().getDescription())
                .weightKg(d.getPackageDetails().getWeightKg())
                .lengthCm(d.getPackageDetails().getLengthCm())
                .widthCm(d.getPackageDetails().getWidthCm())
                .heightCm(d.getPackageDetails().getHeightCm())
                .serviceType(d.getPackageDetails().getServiceType())
                .declaredValue(d.getPackageDetails().getDeclaredValue())
                .build();

        return DeliveryResponse.builder()
                .id(d.getId())
                .trackingNumber(d.getTrackingNumber())
                .customerId(d.getCustomerId())
                .senderAddress(senderDTO)
                .receiverAddress(receiverDTO)
                .packageDetails(packageDTO)
                .status(d.getStatus())
                .charge(d.getCharge())
                .pickupScheduledAt(d.getPickupScheduledAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}