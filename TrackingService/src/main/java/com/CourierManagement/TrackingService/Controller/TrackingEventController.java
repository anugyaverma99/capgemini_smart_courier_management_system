package com.CourierManagement.TrackingService.Controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.CourierManagement.TrackingService.Dto.TrackingEventRequest;
import com.CourierManagement.TrackingService.Dto.TrackingEventResponse;
import com.CourierManagement.TrackingService.Service.TrackingEventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
@Tag(name = "Tracking", description = "Tracking event APIs")
public class TrackingEventController {

 private final TrackingEventService service;

 // add a new tracking event to the timeline
 @PostMapping("/events")
 @Operation(summary = "Add tracking event", description = "Record a new status event in delivery timeline")
 @PreAuthorize("hasRole('ADMIN')")
 public ResponseEntity<TrackingEventResponse> addEvent(
        @Valid @RequestBody TrackingEventRequest request) {
     return ResponseEntity
             .status(HttpStatus.CREATED)
             .body(service.addEvent(request));
 }


 // get full tracking history for a delivery
 @GetMapping("/{trackingNumber}")
 @Operation(summary = "Get timeline", description = "Get full tracking history for a delivery")
 @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
 public ResponseEntity<List<TrackingEventResponse>> getTimeline(
         @PathVariable String trackingNumber) {
     return ResponseEntity.ok(service.getTimeline(trackingNumber));
 }
 // get total number of tracking events
 @GetMapping("/count")
 @Operation(summary = "Get total event count")
 @PreAuthorize("hasRole('ADMIN')")
 public ResponseEntity<Long> getTotalEventCount() {
     return ResponseEntity.ok(service.getTotalEventCount());
 }


 // get the most recent tracking event
 @GetMapping("/{trackingNumber}/latest")
 @Operation(summary = "Get latest status", description = "Get most recent tracking event")
 @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
 public ResponseEntity<TrackingEventResponse> getLatest(
         @PathVariable String trackingNumber) {
     return ResponseEntity.ok(service.getLatestStatus(trackingNumber));
 }
}