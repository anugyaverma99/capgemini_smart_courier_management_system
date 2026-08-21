package com.CourierManagement.TrackingService.Controller;

import com.CourierManagement.TrackingService.Dto.DocumentResponse;
import com.CourierManagement.TrackingService.Service.DocumentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService service;

    private DocumentResponse sampleResponse() {

        return DocumentResponse.builder()
                .id(1L)
                .deliveryId("1")
                .trackingNumber("TRK-1234")
                .fileName("invoice.pdf")
                .filePath("uploads/documents/invoice.pdf")
                .documentType("invoice")
                .contentType("application/pdf")
                .uploadedBy("customer")
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUploadDocument() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "dummy file content".getBytes()
        );

        when(service.uploadDocument(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(sampleResponse());

        mockMvc.perform(multipart("/tracking/documents/upload")
                        .file(file)
                        .param("deliveryId", "1")
                        .param("trackingNumber", "TRK-1234")
                        .param("documentType", "invoice")
                        .param("uploadedBy", "customer"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName")
                        .value("invoice.pdf"))
                .andExpect(jsonPath("$.documentType")
                        .value("invoice"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetDocumentsByDelivery() throws Exception {

        when(service.getDocumentsByDelivery("1"))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/tracking/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deliveryId")
                        .value("1"))
                .andExpect(jsonPath("$[0].fileName")
                        .value("invoice.pdf"));
    }
}