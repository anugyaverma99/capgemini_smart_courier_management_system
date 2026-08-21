package com.CourierManagement.TrackingService.Service;

import com.CourierManagement.TrackingService.Client.DeliveryClient;
import com.CourierManagement.TrackingService.Dto.DocumentResponse;
import com.CourierManagement.TrackingService.Entity.Document;
import com.CourierManagement.TrackingService.Exception.TrackingNotFoundException;
import com.CourierManagement.TrackingService.Repository.DocumentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @InjectMocks
    private DocumentService service;

    @Mock
    private DocumentRepository repository;

    @Mock
    private DeliveryClient deliveryClient;

    @BeforeEach
    void setUp() throws Exception {

        Field uploadDirField =
                DocumentService.class.getDeclaredField("uploadDir");

        uploadDirField.setAccessible(true);
        uploadDirField.set(service, "test-uploads");
    }

    @Test
    void testUploadDocument_success() throws IOException {

        String deliveryId = "1";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy content".getBytes()
        );

        when(deliveryClient.doesDeliveryExist(deliveryId))
                .thenReturn(true);

        when(repository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = service.uploadDocument(
                deliveryId,
                "TRK-1234",
                "invoice",
                "tester",
                file
        );

        assertNotNull(response);
        assertEquals("1", response.getDeliveryId());
        assertEquals("TRK-1234", response.getTrackingNumber());
        assertEquals("test.pdf", response.getFileName());

        Files.deleteIfExists(
                Paths.get(response.getFilePath())
        );
    }

    @Test
    void testUploadDocument_deliveryNotFound() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        when(deliveryClient.doesDeliveryExist("999"))
                .thenReturn(false);

        assertThrows(
                TrackingNotFoundException.class,
                () -> service.uploadDocument(
                        "999",
                        "TRK-999",
                        "invoice",
                        "tester",
                        file
                )
        );

        verify(repository, never()).save(any());
    }

    @Test
    void testGetDocumentsByDelivery_success() {

        Document document = Document.builder()
                .id(1L)
                .deliveryId("1")
                .trackingNumber("TRK-1234")
                .fileName("file.pdf")
                .filePath("test-uploads/file.pdf")
                .documentType("invoice")
                .uploadedBy("tester")
                .build();

        when(deliveryClient.doesDeliveryExist("1"))
                .thenReturn(true);

        when(repository
                .findByDeliveryIdOrderByUploadedAtDesc("1"))
                .thenReturn(List.of(document));

        List<DocumentResponse> docs =
                service.getDocumentsByDelivery("1");

        assertEquals(1, docs.size());
        assertEquals("file.pdf",
                docs.get(0).getFileName());
    }

    @Test
    void testGetDocumentsByDelivery_deliveryNotFound() {

        when(deliveryClient.doesDeliveryExist("999"))
                .thenReturn(false);

        assertThrows(
                TrackingNotFoundException.class,
                () -> service.getDocumentsByDelivery("999")
        );
    }

    @Test
    void testGetDocumentsByDelivery_noDocuments() {

        when(deliveryClient.doesDeliveryExist("1"))
                .thenReturn(true);

        when(repository
                .findByDeliveryIdOrderByUploadedAtDesc("1"))
                .thenReturn(List.of());

        List<DocumentResponse> docs =
                service.getDocumentsByDelivery("1");

        assertNotNull(docs);
        assertTrue(docs.isEmpty());
    }
}