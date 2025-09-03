package com.back.global.controller

import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Health Controller" , description = "서버 상태 확인 API")
class HealthController {
    @GetMapping("/health")
    @Operation(summary = "헬스체크 API")
    fun health(): RsData<Void?> = RsData.of(200, "서버가 정상 작동 중입니다.")

}
