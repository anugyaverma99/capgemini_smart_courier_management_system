package com.CourierManagement.TrackingService.Controller;

import com.CourierManagement.TrackingService.Dto.DeliveryProofResponse;
import com.CourierManagement.TrackingService.Service.DeliveryProofService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeliveryProofController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryProofControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryProofService service;

    private DeliveryProofResponse sampleResponse() {

        return DeliveryProofResponse.builder()
                .id(1L)
                .deliveryId("1")
                .trackingNumber("TRK-1234")
                .receivedBy("Rahul")
                .proofImagePath("uploads/proofs/image.jpg")
                .remarks("Delivered successfully")
                .submittedBy("admin")
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSubmitProof() throws Exception {

        org.springframework.mock.web.MockMultipartFile image =
                new org.springframework.mock.web.MockMultipartFile(
                        "proofImage",
                        "proof.jpg",
                        "image/jpeg",
                        "dummy image".getBytes()
                );

        when(service.submitProof(any(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(multipart("/tracking/proof")
                        .file(image)
                        .param("deliveryId", "1")
                        .param("trackingNumber", "TRK-1234")
                        .param("receivedBy", "Rahul")
                        .param("submittedBy", "admin")
                        .param("remarks", "Delivered successfully"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.deliveryId")
                        .value("1"))
                .andExpect(jsonPath("$.receivedBy")
                        .value("Rahul"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetProof() throws Exception {

        when(service.getProof("1"))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/tracking/1/proof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId")
                        .value("1"))
                .andExpect(jsonPath("$.trackingNumber")
                        .value("TRK-1234"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetProofImage() throws Exception {

        ByteArrayResource resource =
                new ByteArrayResource("dummy image".getBytes());

        when(service.getProofImage("1"))
                .thenReturn(resource);

        mockMvc.perform(get("/tracking/1/proof/image"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("dummy image".getBytes()));
    }
}
