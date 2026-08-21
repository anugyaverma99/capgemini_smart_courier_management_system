package com.CourierManagement.DeliveryService.Dto;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExceptionRequest {
    private String deliveryId;
    private String trackingNumber;
    private String exceptionStatus;  
    private String reason;
}