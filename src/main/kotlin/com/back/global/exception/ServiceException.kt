package com.back.global.exception

import com.back.global.rsData.RsData


/**
 * 서비스 예외를 나타내는 클래스
 * 서비스 계층에서 발생하는 오류를 처리하기 위해 사용
 * @param code 오류 코드
 * @param msg 오류 메시지
 */
class ServiceException(private val code: Int, private val msg: String) :
    RuntimeException("$code : $msg") {

    val rsData: RsData<Void?>
        get() = RsData.of(code, msg)
}
