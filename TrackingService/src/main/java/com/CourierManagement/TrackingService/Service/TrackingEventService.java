package com.CourierManagement.TrackingService.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.CourierManagement.TrackingService.Client.DeliveryClient;
import com.CourierManagement.TrackingService.Dto.TrackingEventRequest;
import com.CourierManagement.TrackingService.Dto.TrackingEventResponse;
import com.CourierManagement.TrackingService.Entity.TrackingEvent;
import com.CourierManagement.TrackingService.Exception.TrackingNotFoundException;
import com.CourierManagement.TrackingService.Repository.TrackingEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.CourierManagement.TrackingService.Config.RabbitMQConfig;

@Service
@RequiredArgsConstructor
public class TrackingEventService {

 private final TrackingEventRepository repository;
 private final DeliveryClient deliveryClient;
 private final RabbitTemplate rabbitTemplate;

 // add a new tracking event
 public TrackingEventResponse addEvent(TrackingEventRequest request) {
	 // verify delivery exists via feign
	 boolean exists = deliveryClient.doesDeliveryExist(request.getDeliveryId());
     if (!exists) {
         throw new TrackingNotFoundException(
             "Delivery not found with ID: " + request.getDeliveryId());
     }
	 // convert request to entity
     TrackingEvent event = TrackingEvent.builder()
             .deliveryId(request.getDeliveryId())
             .trackingNumber(request.getTrackingNumber())
             .status(request.getStatus())
             .location(request.getLocation())
             .remarks(request.getRemarks())
             .updatedBy(request.getUpdatedBy())
             .eventTime(LocalDateTime.now())
             .build();

     // save event to database
     TrackingEvent savedEvent = repository.save(event);
try {
     // send email notification via rabbitmq
         Map<String, Object> delivery = deliveryClient.getDeliveryById(request.getDeliveryId());
         if (delivery != null && delivery.containsKey("receiverAddress")) {
             Map<String, Object> receiver = (Map<String, Object>) delivery.get("receiverAddress");
             String email = (String) receiver.get("email");
             String name = (String) receiver.get("name");
             
             if (email != null && !email.isEmpty()) {
                 Map<String, Object> notificationEvent = Map.of(
                     "trackingNumber", request.getTrackingNumber(),
                     "status", request.getStatus(),
                     "customerEmail", email,
                     "customerName", name != null ? name : "Customer",
                     "remarks", request.getRemarks() != null ? request.getRemarks() : ""
                 );
                 rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, notificationEvent);
             }
         }
     } catch (Exception e) {
         // Log the error but don't fail the tracking update
         System.err.println("Failed to publish email notification event: " + e.getMessage());
     }

     return toResponse(savedEvent);
 }
 // get total count of tracking events
 public long getTotalEventCount() {
	    return repository.count();
	}
 
 // get full timeline for a tracking number
 public List<TrackingEventResponse> getTimeline(String trackingNumber) {
     List<TrackingEvent> events =
             repository.findByTrackingNumberOrderByEventTimeAsc(trackingNumber);

     if (events.isEmpty()) {
         throw new TrackingNotFoundException(
             "No tracking events found for: " + trackingNumber);
     }
     return events.stream().map(this::toResponse).collect(Collectors.toList());
 }

 // get the latest status event
 public TrackingEventResponse getLatestStatus(String trackingNumber) {
     TrackingEvent event =
             repository.findTopByTrackingNumberOrderByEventTimeDesc(trackingNumber);

     if (event == null) {
         throw new TrackingNotFoundException(
             "No tracking events found for: " + trackingNumber);
     }
     return toResponse(event);
 }

 
 // convert entity to response dto
 private TrackingEventResponse toResponse(TrackingEvent e) {
	 // entity to dto conversion
     return TrackingEventResponse.builder()
             .id(e.getId())
             .deliveryId(e.getDeliveryId())
             .trackingNumber(e.getTrackingNumber())
             .status(e.getStatus())
             .location(e.getLocation())
             .remarks(e.getRemarks())
             .updatedBy(e.getUpdatedBy())
             .eventTime(e.getEventTime())
             .createdAt(e.getCreatedAt())
             .build();
 }
}
