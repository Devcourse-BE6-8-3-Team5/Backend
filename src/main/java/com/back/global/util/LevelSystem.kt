package com.back.global.util

import com.back.global.exception.ServiceException

object LevelSystem {
    @JvmStatic
    fun calculateLevel(exp: Int): Int {
        if (exp < 0) {
            throw ServiceException(400, "경험치는 음수가 될 수 없습니다.")
        }

        return when {
            exp < 100 -> 1
            exp < 200 -> 2
            else -> 3
        }
    }

    @JvmStatic
    fun getImageByLevel(level: Int): String {
        return when (level) {
            1 -> "" // 레벨 1 이미지 URL ,src/main/resources/static/images에 저장
            2 -> "" // 레벨 2 이미지 URL
            3 -> "" // 레벨 3 이미지 URL
            else -> throw ServiceException(400, "유효하지 않은 레벨입니다.")
        }
    }
}
