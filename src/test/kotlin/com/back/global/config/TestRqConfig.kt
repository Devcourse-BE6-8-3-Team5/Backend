package com.back.global.config

import com.back.global.rq.Rq
import com.back.global.rq.TestRq
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestRqConfig {
    @Bean
    @Primary
    fun testRq(): Rq {
        return TestRq()
    }
}
