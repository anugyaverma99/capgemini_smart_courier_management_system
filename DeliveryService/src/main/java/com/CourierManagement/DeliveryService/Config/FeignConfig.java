package com.CourierManagement.DeliveryService.Config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor forwardAuthHeaders() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                String email = attributes.getRequest().getHeader("X-User-Email");
                String role  = attributes.getRequest().getHeader("X-User-Role");

                if (email != null) {
                    requestTemplate.header("X-User-Email", email);
                }
                if (role != null) {
                    requestTemplate.header("X-User-Role", role);
                }
            }
        };
    }
}
