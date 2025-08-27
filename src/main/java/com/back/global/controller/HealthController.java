package com.back.global.controller;

import com.back.global.rsData.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public RsData<Void> health() {
        return RsData.of(200, "서버가 정상 작동 중입니다.");
    }
}
