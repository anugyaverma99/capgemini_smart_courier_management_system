package com.CourierManagement.TrackingService.Service;

import com.CourierManagement.TrackingService.Client.DeliveryClient;
import com.CourierManagement.TrackingService.Dto.TrackingEventRequest;
import com.CourierManagement.TrackingService.Dto.TrackingEventResponse;
import com.CourierManagement.TrackingService.Entity.TrackingEvent;
import com.CourierManagement.TrackingService.Entity.TrackingStatus;
import com.CourierManagement.TrackingService.Exception.TrackingNotFoundException;
import com.CourierManagement.TrackingService.Repository.TrackingEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingEventServiceTest {

    @Mock
    private TrackingEventRepository repository;

    @Mock
    private DeliveryClient deliveryClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TrackingEventService service;

    private TrackingEvent testEvent;
    private TrackingEventRequest request;

    @BeforeEach
    void setUp() {

        LocalDateTime fixedTime =
                LocalDateTime.of(2026, 3, 29, 18, 0);

        testEvent = TrackingEvent.builder()
                .id(1L)
                .deliveryId("1")
                .trackingNumber("TRK-TEST001")
                .status(TrackingStatus.PICKED_UP)
                .location("Mumbai Hub")
                .remarks("Picked up")
                .updatedBy("admin")
                .eventTime(fixedTime)
                .createdAt(fixedTime)
                .build();

        request = TrackingEventRequest.builder()
                .deliveryId("1")
                .trackingNumber("TRK-TEST001")
                .status(TrackingStatus.PICKED_UP)
                .location("Mumbai Hub")
                .remarks("Picked up")
                .updatedBy("admin")
                .build();
    }

    @Test
    void addEvent_success() {

        when(deliveryClient.doesDeliveryExist("1"))
                .thenReturn(true);

        when(repository.save(any(TrackingEvent.class)))
                .thenReturn(testEvent);

        
        when(deliveryClient.getDeliveryById("1"))
                .thenReturn(null);

        TrackingEventResponse response =
                service.addEvent(request);

        assertNotNull(response);
        assertEquals(
                "TRK-TEST001",
                response.getTrackingNumber()
        );

        verify(repository)
                .save(any(TrackingEvent.class));
    }

    @Test
    void addEvent_deliveryNotFound_throwsException() {

        when(deliveryClient.doesDeliveryExist("1"))
                .thenReturn(false);

        assertThrows(
                TrackingNotFoundException.class,
                () -> service.addEvent(request)
        );

        verify(repository, never())
                .save(any());
    }

    @Test
    void getTimeline_success() {

        when(repository
                .findByTrackingNumberOrderByEventTimeAsc(
                        "TRK-TEST001"
                ))
                .thenReturn(List.of(testEvent));

        List<TrackingEventResponse> responses =
                service.getTimeline("TRK-TEST001");

        assertEquals(1, responses.size());
    }

    @Test
    void getTimeline_notFound() {

        when(repository
                .findByTrackingNumberOrderByEventTimeAsc(
                        "INVALID"
                ))
                .thenReturn(List.of());

        assertThrows(
                TrackingNotFoundException.class,
                () -> service.getTimeline("INVALID")
        );
    }

    @Test
    void getLatestStatus_success() {

        when(repository
                .findTopByTrackingNumberOrderByEventTimeDesc(
                        "TRK-TEST001"
                ))
                .thenReturn(testEvent);

        TrackingEventResponse response =
                service.getLatestStatus("TRK-TEST001");

        assertEquals(
                TrackingStatus.PICKED_UP,
                response.getStatus()
        );
    }

    @Test
    void getLatestStatus_notFound() {

        when(repository
                .findTopByTrackingNumberOrderByEventTimeDesc(
                        "INVALID"
                ))
                .thenReturn(null);

        assertThrows(
                TrackingNotFoundException.class,
                () -> service.getLatestStatus("INVALID")
        );
    }

    @Test
    void getTotalEventCount_success() {

        when(repository.count()).thenReturn(5L);

        long count = service.getTotalEventCount();

        assertEquals(5L, count);
    }
}