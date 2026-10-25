package com.project.bookngo.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
@Data
public class RateLimitConfig {
    @Value("${rate-limit.capacity:5}")
    private int capacity;

    @Value("${rate-limit.leak-rate-ms:60000}")
    private long leakRateMs;


}
