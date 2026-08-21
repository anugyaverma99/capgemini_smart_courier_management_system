package com.CourierManagement.TrackingService.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.CourierManagement.TrackingService.Client.DeliveryClient;
import com.CourierManagement.TrackingService.Dto.DeliveryProofRequest;
import com.CourierManagement.TrackingService.Dto.DeliveryProofResponse;
import com.CourierManagement.TrackingService.Entity.DeliveryProof;
import com.CourierManagement.TrackingService.Exception.TrackingNotFoundException;
import com.CourierManagement.TrackingService.Repository.DeliveryProofRepository;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

@Service
@RequiredArgsConstructor
public class DeliveryProofService {

    private final DeliveryProofRepository repository;
    private final DeliveryClient deliveryClient;  //  injected via RequiredArgsConstructor

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    // submit proof of delivery
    public DeliveryProofResponse submitProof(
            DeliveryProofRequest request,
            MultipartFile proofImage) throws IOException {

        // ── Feign call: verify delivery exists before submitting proof ──
        boolean exists = deliveryClient.doesDeliveryExist(
                request.getDeliveryId());
        if (!exists) {
            throw new TrackingNotFoundException(
                "Cannot submit proof — delivery not found with ID: "
                + request.getDeliveryId());
        }

        String imagePath = null;

        // save proof image to disk if provided
        if (proofImage != null && !proofImage.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir, "proofs");
            Files.createDirectories(uploadPath);
            String uniqueName = UUID.randomUUID() + "_" + proofImage.getOriginalFilename();
            Path targetPath   = uploadPath.resolve(uniqueName);
            Files.copy(proofImage.getInputStream(), targetPath,
                       StandardCopyOption.REPLACE_EXISTING);
            imagePath = targetPath.toString();
        }
        // build proof entity
        DeliveryProof proof = DeliveryProof.builder()
                .deliveryId(request.getDeliveryId())
                .trackingNumber(request.getTrackingNumber())
                .receivedBy(request.getReceivedBy())
                .proofImagePath(imagePath)
                .remarks(request.getRemarks())
                .submittedBy(request.getSubmittedBy())
                .deliveredAt(request.getDeliveredAt())
                .build();
        // save and return
        return toResponse(repository.save(proof));
    }

    // get proof for a delivery
    public DeliveryProofResponse getProof(String deliveryId) {

        // ── Feign call: verify delivery exists before fetching proof ──
        boolean exists = deliveryClient.doesDeliveryExist(
                deliveryId);
        if (!exists) {
            throw new TrackingNotFoundException(
                "Cannot fetch proof — delivery not found with ID: " + deliveryId);
        }

        return repository.findByDeliveryId(deliveryId)
                .map(this::toResponse)
                .orElseThrow(() -> new TrackingNotFoundException(
                        "No delivery proof found for: " + deliveryId));
    }

    // download the proof image from disk
    public Resource getProofImage(String deliveryId) throws IOException {
        DeliveryProof proof = repository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new TrackingNotFoundException(
                        "No delivery proof found for: " + deliveryId));

        if (proof.getProofImagePath() == null || proof.getProofImagePath().isBlank()) {
            throw new TrackingNotFoundException(
                    "No proof image found for: " + deliveryId);
        }

        Path imagePath = Paths.get(proof.getProofImagePath()).normalize();
        Resource resource = new UrlResource(imagePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new TrackingNotFoundException(
                    "Proof image is not available for: " + deliveryId);
        }
        return resource;
    }
    // convert entity to response dto
    private DeliveryProofResponse toResponse(DeliveryProof p) {
        return DeliveryProofResponse.builder()
                .id(p.getId())
                .deliveryId(p.getDeliveryId())
                .trackingNumber(p.getTrackingNumber())
                .receivedBy(p.getReceivedBy())
                .proofImagePath(p.getProofImagePath())
                .remarks(p.getRemarks())
                .submittedBy(p.getSubmittedBy())
                .deliveredAt(p.getDeliveredAt() != null
                        ? p.getDeliveredAt()
                        : LocalDateTime.now())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
