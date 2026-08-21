package com.CourierManagement.TrackingService.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.CourierManagement.TrackingService.Client.DeliveryClient;
import com.CourierManagement.TrackingService.Dto.DocumentResponse;
import com.CourierManagement.TrackingService.Entity.Document;
import com.CourierManagement.TrackingService.Exception.TrackingNotFoundException;
import com.CourierManagement.TrackingService.Repository.DocumentRepository;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository repository;
    private final DeliveryClient deliveryClient;  // ← injected via RequiredArgsConstructor

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    // upload and store a document file
    public DocumentResponse uploadDocument(
            String deliveryId,
            String trackingNumber,
            String documentType,
            String uploadedBy,
            MultipartFile file) throws IOException {

        // verify delivery exists before uploading
        boolean exists = deliveryClient.doesDeliveryExist(deliveryId);
        if (!exists) {
            throw new TrackingNotFoundException(
                "Cannot upload document — delivery not found with ID: " + deliveryId);
        }

        // create upload directory
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // generate unique filename
        String originalName = file.getOriginalFilename();
        String uniqueName   = UUID.randomUUID() + "_" + originalName;
        Path   targetPath   = uploadPath.resolve(uniqueName);

        // save file to disk
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // save document record in database
        Document doc = Document.builder()
                .deliveryId(deliveryId)
                .trackingNumber(trackingNumber)
                .fileName(originalName)
                .filePath(targetPath.toString())
                .documentType(documentType)  // e.g. invoice, label, proof of delivery
                .contentType(file.getContentType())  // what kind of file format it is
                .uploadedBy(uploadedBy)
                .build();

        // save and return response
        return toResponse(repository.save(doc));
    }

    // get all documents for a delivery
    public List<DocumentResponse> getDocumentsByDelivery(String deliveryId) {

        // verify delivery exists
        boolean exists = deliveryClient.doesDeliveryExist(deliveryId);
        if (!exists) {
            throw new TrackingNotFoundException(
                "Cannot fetch documents — delivery not found with ID: " + deliveryId);
        }
        return repository.findByDeliveryIdOrderByUploadedAtDesc(deliveryId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        }

    // convert entity to response dto
    private DocumentResponse toResponse(Document d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .deliveryId(d.getDeliveryId())
                .trackingNumber(d.getTrackingNumber())
                .fileName(d.getFileName())
                .filePath(d.getFilePath())
                .documentType(d.getDocumentType())
                .contentType(d.getContentType())
                .uploadedBy(d.getUploadedBy())
                .uploadedAt(d.getUploadedAt())
                .build();
    }
}