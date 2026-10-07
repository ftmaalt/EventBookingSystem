package com.project.bookngo.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedList;

@Configuration
@Data
public class RateLimitConfig {
    private int capacity = 5;
    private long leakRateMs = 60000;


}
