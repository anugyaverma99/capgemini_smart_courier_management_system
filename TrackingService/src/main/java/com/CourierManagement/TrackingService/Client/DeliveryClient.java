package com.CourierManagement.TrackingService.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.CourierManagement.TrackingService.Config.FeignConfig;

@FeignClient(name = "delivery-service",configuration=FeignConfig.class)
public interface DeliveryClient {

    @GetMapping("/deliveries/{id}/exists")
    boolean doesDeliveryExist(@PathVariable("id") String id);

    @GetMapping("/deliveries/{id}")
    java.util.Map<String, Object> getDeliveryById(@PathVariable("id") String id);
}
