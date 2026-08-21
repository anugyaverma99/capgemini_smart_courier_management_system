package com.CourierManagement.DeliveryService.Controller;

import com.CourierManagement.DeliveryService.Dto.*;
import com.CourierManagement.DeliveryService.Entity.DeliveryStatus;
import com.CourierManagement.DeliveryService.Service.DeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeliveryController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryService service;


    private DeliveryResponse sampleResponse() {

        return DeliveryResponse.builder()
                .id(1L)
                .trackingNumber("TRK-1234ABCD")
                .customerId("cust123")

                .senderAddress(
                        AddressDto.builder()
                                .name("Sender")
                                .build()
                )

                .receiverAddress(
                        AddressDto.builder()
                                .name("Receiver")
                                .build()
                )

                .packageDetails(
                        PackageDto.builder()
                                .description("Sample Package")
                                .build()
                )

                .status(DeliveryStatus.DRAFT)
                .charge(100.0)
                .pickupScheduledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    private CreateDeliveryRequest sampleCreateRequest() {

        return CreateDeliveryRequest.builder()
                .customerId("cust123")

                .senderAddress(
                        AddressDto.builder()
                                .name("Sender")
                                .email("sender@example.com")
                                .phone("9876543210")
                                .addressLine("123 MG Road")
                                .city("Mumbai")
                                .state("Maharashtra")
                                .zipCode("400001")
                                .country("India")
                                .build()
                )

                .receiverAddress(
                        AddressDto.builder()
                                .name("Receiver")
                                .email("receiver@example.com")
                                .phone("9123456780")
                                .addressLine("456 MG Road")
                                .city("Mumbai")
                                .state("Maharashtra")
                                .zipCode("400002")
                                .country("India")
                                .build()
                )

                .packageDetails(
                        PackageDto.builder()
                                .description("Sample Package")
                                .weightKg(2.0)
                                .lengthCm(20)
                                .widthCm(20)
                                .heightCm(10)
                                .serviceType("express")
                                .declaredValue(5000)
                                .build()
                )

                .pickupScheduledAt(LocalDateTime.now())
                .build();
    }


    private UpdateStatusRequest sampleStatusRequest() {

        return UpdateStatusRequest.builder()
                .status(DeliveryStatus.BOOKED)
                .build();
    }


    // 1. CREATE DELIVERY

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCreateDelivery() throws Exception {

        CreateDeliveryRequest request = sampleCreateRequest();

        DeliveryResponse response = sampleResponse();

        when(service.createDelivery(any(CreateDeliveryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.trackingNumber")
                .value("TRK-1234ABCD"))
        .andExpect(jsonPath("$.customerId")
                .value("cust123"));

        verify(service, times(1))
                .createDelivery(any(CreateDeliveryRequest.class));
    }


    // 2. GET MY DELIVERIES

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetMyDeliveries() throws Exception {

        List<DeliveryResponse> deliveries =
                List.of(sampleResponse());

        when(service.getMyDeliveries("cust123"))
                .thenReturn(deliveries);

        mockMvc.perform(
                get("/deliveries/my")
                        .header("X-User-Email", "cust123")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].customerId")
                .value("cust123"));

        verify(service, times(1))
                .getMyDeliveries("cust123");
    }


    // 3. GET DELIVERY BY ID - CUSTOMER

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetByIdAsCustomer() throws Exception {

        DeliveryResponse response = sampleResponse();

        when(service.getById(1L))
                .thenReturn(response);

        mockMvc.perform(
                get("/deliveries/1")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.trackingNumber")
                .value("TRK-1234ABCD"));

        verify(service, times(1))
                .getById(1L);
    }


    // 4. GET DELIVERY BY ID - ADMIN

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetByIdAsAdmin() throws Exception {

        DeliveryResponse response = sampleResponse();

        when(service.getById(1L))
                .thenReturn(response);

        mockMvc.perform(
                get("/deliveries/1")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));

        verify(service, times(1))
                .getById(1L);
    }


    // 5. TRACK DELIVERY

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetByTrackingNumber() throws Exception {

        DeliveryResponse response = sampleResponse();

        when(service.getByTrackingNumber("TRK-1234ABCD"))
                .thenReturn(response);

        mockMvc.perform(
                get("/deliveries/track/TRK-1234ABCD")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trackingNumber")
                .value("TRK-1234ABCD"));

        verify(service, times(1))
                .getByTrackingNumber("TRK-1234ABCD");
    }


    // 6. UPDATE STATUS

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateStatus() throws Exception {

        UpdateStatusRequest request =
                sampleStatusRequest();

        DeliveryResponse response =
                sampleResponse();

        response.setStatus(DeliveryStatus.BOOKED);

        when(service.updateStatus(
                eq(1L),
                any(UpdateStatusRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                put("/deliveries/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status")
                .value("BOOKED"));

        verify(service, times(1))
                .updateStatus(
                        eq(1L),
                        any(UpdateStatusRequest.class)
                );
    }


    // 7. CHECK DELIVERY EXISTS

    @Test
    void testDoesDeliveryExist() throws Exception {

        when(service.getById(1L))
                .thenReturn(sampleResponse());

        mockMvc.perform(
                get("/deliveries/1/exists")
        )
        .andExpect(status().isOk())
        .andExpect(content().string("true"));

        verify(service, times(1))
                .getById(1L);
    }


    // 8. CHECK DELIVERY DOES NOT EXIST

    @Test
    void testDoesDeliveryNotExist() throws Exception {

        when(service.getById(99L))
                .thenThrow(
                        new RuntimeException("Delivery not found")
                );

        mockMvc.perform(
                get("/deliveries/99/exists")
        )
        .andExpect(status().isOk())
        .andExpect(content().string("false"));

        verify(service, times(1))
                .getById(99L);
    }


    // 9. GET ALL DELIVERIES

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllDeliveries() throws Exception {

        List<DeliveryResponse> deliveries =
                List.of(sampleResponse());

        when(service.getAllDeliveries())
                .thenReturn(deliveries);

        mockMvc.perform(
                get("/deliveries")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].trackingNumber")
                .value("TRK-1234ABCD"));

        verify(service, times(1))
                .getAllDeliveries();
    }
}