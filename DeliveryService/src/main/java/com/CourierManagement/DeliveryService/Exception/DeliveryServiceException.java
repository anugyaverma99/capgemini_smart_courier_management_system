package com.CourierManagement.DeliveryService.Exception;


import lombok.Getter;

@Getter
public class DeliveryServiceException extends RuntimeException {
    private final int statusCode;

    public DeliveryServiceException(String message) {
        super(message);
        this.statusCode = 404;
    }

    public DeliveryServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}