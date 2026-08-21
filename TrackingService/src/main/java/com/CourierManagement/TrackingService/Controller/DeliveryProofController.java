package com.CourierManagement.TrackingService.Controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.CourierManagement.TrackingService.Dto.DeliveryProofRequest;
import com.CourierManagement.TrackingService.Dto.DeliveryProofResponse;
import com.CourierManagement.TrackingService.Service.DeliveryProofService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
@Tag(name = "Delivery Proof", description = "Delivery proof APIs")
public class DeliveryProofController {

 private final DeliveryProofService service;


 // submit proof of delivery with optional image
 @PostMapping(value = "/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 @PreAuthorize("hasRole('ADMIN')")
 @Operation(summary = "Submit proof", description = "Rider submits delivery proof at point of delivery")
 public ResponseEntity<DeliveryProofResponse> submitProof(
         @RequestParam("deliveryId")     String deliveryId,
         @RequestParam("trackingNumber") String trackingNumber,
         @RequestParam("receivedBy")     String receivedBy,
         @RequestParam("submittedBy")    String submittedBy,
         @RequestParam(value = "remarks", required = false) String remarks,
         @RequestParam(value = "proofImage", required = false) MultipartFile proofImage)
         throws IOException {

     DeliveryProofRequest request = DeliveryProofRequest.builder()
             .deliveryId(deliveryId)
             .trackingNumber(trackingNumber)
             .receivedBy(receivedBy)
             .submittedBy(submittedBy)
             .remarks(remarks)
             .build();

     return ResponseEntity
             .status(HttpStatus.CREATED)
             .body(service.submitProof(request, proofImage));
 }

  @GetMapping("/{deliveryId}/proof")
  @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
  @Operation(summary = "Get proof", description = "Get delivery proof — shown on confirmation page")
  
  public ResponseEntity<DeliveryProofResponse> getProof(
         @PathVariable String deliveryId) {
     return ResponseEntity.ok(service.getProof(deliveryId));
 }

  @GetMapping("/{deliveryId}/proof/image")
  @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
  @Operation(summary = "Get proof image", description = "Download the saved proof image for a delivery")
  public ResponseEntity<Resource> getProofImage(
          @PathVariable String deliveryId) throws IOException {
      Resource image = service.getProofImage(deliveryId);
      return ResponseEntity.ok()
              .contentType(MediaType.APPLICATION_OCTET_STREAM)
              .body(image);
  }
}
