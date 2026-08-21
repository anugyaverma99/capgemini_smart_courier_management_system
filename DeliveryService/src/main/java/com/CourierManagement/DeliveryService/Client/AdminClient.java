package com.CourierManagement.DeliveryService.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.CourierManagement.DeliveryService.Dto.DeliveryMonitorRequest;
import com.CourierManagement.DeliveryService.Dto.ExceptionRequest;

@FeignClient(name = "admin-service")
public interface AdminClient {

	
    @PostMapping("/admin/deliveries/sync")
    void syncDelivery(@RequestBody DeliveryMonitorRequest request);
    @PostMapping("/admin/exceptions")
    void raiseException(@RequestBody ExceptionRequest request);
}

