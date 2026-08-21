package com.CourierManagement.TrackingService.Controller;

import com.CourierManagement.TrackingService.Dto.TrackingEventRequest;
import com.CourierManagement.TrackingService.Dto.TrackingEventResponse;
import com.CourierManagement.TrackingService.Entity.TrackingStatus;
import com.CourierManagement.TrackingService.Service.TrackingEventService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrackingEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrackingEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackingEventService service;

    @Autowired
    private ObjectMapper objectMapper;

    private TrackingEventRequest sampleRequest() {
        return TrackingEventRequest.builder()
                .deliveryId("1")
                .trackingNumber("TRK-1234")
                .status(TrackingStatus.IN_TRANSIT)
                .location("Mumbai Hub")
                .remarks("Package reached Mumbai Hub")
                .updatedBy("admin")
                .build();
    }

    private TrackingEventResponse sampleResponse() {
        return TrackingEventResponse.builder()
                .id(1L)
                .deliveryId("1")
                .trackingNumber("TRK-1234")
                .status(TrackingStatus.IN_TRANSIT)
                .location("Mumbai Hub")
                .remarks("Package reached Mumbai Hub")
                .updatedBy("admin")
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAddEvent() throws Exception {

        when(service.addEvent(any(TrackingEventRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/tracking/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.trackingNumber")
                        .value("TRK-1234"))
                .andExpect(jsonPath("$.status")
                        .value("IN_TRANSIT"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetTimeline() throws Exception {

        when(service.getTimeline("TRK-1234"))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/tracking/TRK-1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackingNumber")
                        .value("TRK-1234"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTotalEventCount() throws Exception {

        when(service.getTotalEventCount()).thenReturn(10L);

        mockMvc.perform(get("/tracking/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetLatestStatus() throws Exception {

        when(service.getLatestStatus("TRK-1234"))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/tracking/TRK-1234/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber")
                        .value("TRK-1234"))
                .andExpect(jsonPath("$.status")
                        .value("IN_TRANSIT"));
    }
}