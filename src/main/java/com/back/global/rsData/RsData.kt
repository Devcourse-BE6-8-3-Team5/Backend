package com.back.global.rsData

class RsData<T>(@JvmField val code: Int, @JvmField val message: String, @JvmField val data: T?) {
    companion object {
        @JvmStatic
        fun <T> of(code: Int, message: String, data: T): RsData<T> {
            return RsData(code, message, data)
        }

        @JvmStatic
        fun <T> of(code: Int, message: String): RsData<T?> {
            return RsData(code, message, null)
        }

        //성공 편의 메소드
        fun <T> successOf(data: T): RsData<T> {
            return of(200, "success", data)
        }

        //실패 편의 메소드
        fun <T> failOf(data: T): RsData<T> {
            return of(500, "fail", data)
        }

        // 실패 편의 메소드 (메시지 포함)
        fun <T> failOf(message: String): RsData<T?> {
            return of(500, message, null)
        }
    }
}
