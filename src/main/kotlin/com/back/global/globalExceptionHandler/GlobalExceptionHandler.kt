package com.back.global.globalExceptionHandler

import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import com.fasterxml.jackson.core.JsonProcessingException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.io.IOException


/**
 * 글로벌 예외 핸들러 클래스
 * 각 예외에 대한 적절한 HTTP 상태 코드와 메시지를 포함한 응답 반환
 * 400: Bad Request
 * 404: Not Found
 * 500: Internal Server Error
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    // ServiceException: 서비스 계층에서 발생하는 커스텀 예외
    @ExceptionHandler(ServiceException::class)
    fun handle(ex: ServiceException): ResponseEntity<RsData<Void?>> {
        val rsData = ex.rsData
        val statusCode = rsData.code
        val status = HttpStatus.resolve(statusCode) ?: HttpStatus.INTERNAL_SERVER_ERROR

        return ResponseEntity.status(status).body(rsData)
    }

    // NoSuchElementException: 데이터 없을떄 예외
    @ExceptionHandler(NoSuchElementException::class)
    fun handle(ex: NoSuchElementException): ResponseEntity<RsData<Void?>> {
        return ResponseEntity(
            RsData.of(
                404,
                "해당 데이터가 존재하지 않습니다"
            ),
            HttpStatus.NOT_FOUND
        )
    }

    //ConstraintViolationException: 제약 조건(@NotNull, @Size 등)을 어겼을 때 발생예외
    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(ex: ConstraintViolationException): ResponseEntity<RsData<Void?>> {
        //메세지 형식 : <필드명>-<검증어노테이션명>-<검증실패메시지>
        val message = ex.constraintViolations
            .map { violation ->
                val path = violation.propertyPath.toString()
                val field = path.substringAfter('.', path) // '.' 있으면 두 번째 부분, 없으면 전체
                val messageTemplateBits = violation.messageTemplate.split('.')
                val code = if (messageTemplateBits.size >= 2)
                    messageTemplateBits[messageTemplateBits.size - 2]
                else
                    "Unknown"
                "${field}-${code}-${violation.message}"
            }
            .sorted()
            .joinToString("\n") { it }

        return ResponseEntity(
            RsData.of(
                400,
                message
            ),
            HttpStatus.BAD_REQUEST
        )
    }


    // MethodArgumentNotValidException: @Valid 어노테이션을 사용한 유효성 검사 실패시 발생하는 예외
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(ex: MethodArgumentNotValidException): ResponseEntity<RsData<Void?>> {
        //메세지 형식 : <필드명>-<검증어노테이션명>-<검증실패메시지>
        val message = ex.bindingResult
            .allErrors
            .filter { it is FieldError }
            .map{ it as FieldError }
            .map { error -> "${error.field}-${error.code}-${error.defaultMessage}" }
            .sorted()
            .joinToString("\n") { it }

        return ResponseEntity(
            RsData.of(
                400,
                message
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    // HttpMessageNotReadableException : 요청 본문이 올바르지 않을 때 발생하는 예외
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ex: HttpMessageNotReadableException?): ResponseEntity<RsData<Void?>> {
        return ResponseEntity(
            RsData.of(
                400,
                "요청 본문이 올바르지 않습니다."
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    // MissingRequestHeaderException : 필수 요청 헤더가 누락되었을 때 발생하는 예외
    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handle(ex: MissingRequestHeaderException): ResponseEntity<RsData<Void?>> {
        // 메세지 형식 : <필드명>-<검증어노테이션명>-<검증실패메시지>
        val message = "${ex.headerName}-NotBlank-${ex.localizedMessage}"

        return ResponseEntity(
            RsData.of(
                400,
                message
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(JsonProcessingException::class)
    fun handleJsonProcessingException(e: JsonProcessingException): ResponseEntity<RsData<Void?>> {
        return ResponseEntity.badRequest()
            .body(RsData.of(400, "JSON 파싱 오류가 발생했습니다.", null))
    }

    @ExceptionHandler(IOException::class)
    fun handleIOException(e: IOException): ResponseEntity<RsData<Void?>> {
        return ResponseEntity.internalServerError()
            .body(RsData.of(500, "서버 내부 오류가 발생했습니다.", null))
    }
}
