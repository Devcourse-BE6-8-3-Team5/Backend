package com.back.backend.global.config;

import com.back.backend.global.rq.TestRq;
import com.back.global.rq.Rq;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestRqConfig {
    @Bean
    @Primary
    public Rq testRq() {
        return new TestRq();
    }
}
