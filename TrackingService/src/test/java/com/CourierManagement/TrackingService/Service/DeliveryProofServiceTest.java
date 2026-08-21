package com.CourierManagement.TrackingService.Service;

import com.CourierManagement.TrackingService.Client.DeliveryClient;
import com.CourierManagement.TrackingService.Dto.DeliveryProofRequest;
import com.CourierManagement.TrackingService.Dto.DeliveryProofResponse;
import com.CourierManagement.TrackingService.Entity.DeliveryProof;
import com.CourierManagement.TrackingService.Exception.TrackingNotFoundException;
import com.CourierManagement.TrackingService.Repository.DeliveryProofRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryProofServiceTest {

    @Mock
    private DeliveryProofRepository repository;

    @Mock
    private DeliveryClient deliveryClient;

    @InjectMocks
    private DeliveryProofService service;

    @BeforeEach
    void setUp() throws Exception {

        Field uploadDirField =
                DeliveryProofService.class
                        .getDeclaredField("uploadDir");

        uploadDirField.setAccessible(true);
        uploadDirField.set(service, "test-uploads");
    }

    private DeliveryProofRequest sampleRequest() {

        return DeliveryProofRequest.builder()
                .deliveryId("1")
                .trackingNumber("TRK-1234")
                .receivedBy("Rahul")
                .submittedBy("admin")
                .remarks("Delivered")
                .deliveredAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testSubmitProof_success() throws Exception {

        DeliveryProofRequest request = sampleRequest();

        MockMultipartFile image =
                new MockMultipartFile(
                        "proofImage",
                        "proof.jpg",
                        "image/jpeg",
                        "dummy image".getBytes()
                );

        when(deliveryClient.doesDeliveryExist("1"))
                .thenReturn(true);

        when(repository.save(any(DeliveryProof.class)))
                .thenAnswer(invocation -> {
                    DeliveryProof proof =
                            invocation.getArgument(0);

                    proof.setId(1L);
                    return proof;
                });

        DeliveryProofResponse response =
                service.submitProof(request, image);

        assertNotNull(response);
        assertEquals("1", response.getDeliveryId());
        assertEquals("TRK-1234",
                response.getTrackingNumber());

        verify(repository, times(1))
                .save(any(DeliveryProof.class));

        if (response.getProofImagePath() != null) {
            Files.deleteIfExists(
                    Path.of(response.getProofImagePath())
            );
        }
    }

    @Test
    void testSubmitProof_deliveryNotFound() {

        when(deliveryClient.doesDeliveryExist("999"))
                .thenReturn(false);

        DeliveryProofRequest request =
                DeliveryProofRequest.builder()
                        .deliveryId("999")
                        .build();

        assertThrows(
                TrackingNotFoundException.class,
                () -> service.submitProof(request, null)
        );

        verify(repository, never())
                .save(any());
    }

    @Test
    void testGetProof_success() {

        DeliveryProof proof =
                DeliveryProof.builder()
                        .id(1L)
                        .deliveryId("1")
                        .trackingNumber("TRK-1234")
                        .receivedBy("Rahul")
                        .submittedBy("admin")
                        .build();

        when(deliveryClient.doesDeliveryExist("1"))
                .thenReturn(true);

        when(repository.findByDeliveryId("1"))
                .thenReturn(Optional.of(proof));

        DeliveryProofResponse response =
                service.getProof("1");

        assertNotNull(response);
        assertEquals("1", response.getDeliveryId());
    }

    @Test
    void testGetProof_deliveryNotFound() {

        when(deliveryClient.doesDeliveryExist("999"))
                .thenReturn(false);

        assertThrows(
                TrackingNotFoundException.class,
                () -> service.getProof("999")
        );
    }

    @Test
    void testGetProof_notFound() {

        when(deliveryClient.doesDeliveryExist("1"))
                .thenReturn(true);

        when(repository.findByDeliveryId("1"))
                .thenReturn(Optional.empty());

        assertThrows(
                TrackingNotFoundException.class,
                () -> service.getProof("1")
        );
    }

    @Test
    void testGetProofImage_success() throws Exception {

        Path tempFile =
                Files.createTempFile(
                        "proof-test",
                        ".jpg"
                );

        Files.writeString(tempFile, "dummy image");

        DeliveryProof proof =
                DeliveryProof.builder()
                        .id(1L)
                        .deliveryId("1")
                        .proofImagePath(tempFile.toString())
                        .build();

        when(repository.findByDeliveryId("1"))
                .thenReturn(Optional.of(proof));

        Resource resource =
                service.getProofImage("1");

        assertNotNull(resource);
        assertTrue(resource.exists());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testGetProofImage_noImage() {

        DeliveryProof proof =
                DeliveryProof.builder()
                        .deliveryId("1")
                        .proofImagePath(null)
                        .build();

        when(repository.findByDeliveryId("1"))
                .thenReturn(Optional.of(proof));

        assertThrows(
                TrackingNotFoundException.class,
                () -> service.getProofImage("1")
        );
    }
}