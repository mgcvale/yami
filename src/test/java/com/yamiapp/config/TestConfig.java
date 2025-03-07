package com.yamiapp.config;

import com.yamiapp.mock.FakeBackblazeService;
import com.yamiapp.service.BackblazeService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    public BackblazeService backblazeService() {
        return new FakeBackblazeService();
    }

}
