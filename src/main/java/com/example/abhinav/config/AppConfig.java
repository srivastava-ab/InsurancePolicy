package com.example.abhinav.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.abhinav.service.PlanServiceImpl;

@Configuration
public class AppConfig {
    @Bean
    public PlanServiceImpl transferService() {
        return new PlanServiceImpl();
    }
}