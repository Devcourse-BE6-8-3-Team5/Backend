package com.back.global.util

object HtmlEntityDecoder {
    // HTML 엔티티 매핑 테이블
    private val HTML_ENTITIES = mapOf(
        // 기본 HTML 엔티티들
        "&lt;" to "<",
        "&gt;" to ">",
        "&amp;" to "&",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&#39;" to "'",
        "&nbsp;" to " ",

        // HTML 태그들
        "&lt;b&gt;" to "",
        "&lt;/b&gt;" to "",
        "\\u003Cb\\u003E" to "",
        "\\u003C/b\\u003E" to "",
        "\\u003C" to "<",
        "\\u003E" to ">",

        // 자주 사용되는 특수문자들
        "&mdash;" to "—",
        "&ndash;" to "–",
        "&lsquo;" to "'",
        "&rsquo;" to "'",
        "&ldquo;" to "\"",
        "&rdquo;" to "\"",
        "&hellip;" to "…"
    )

    fun decode(encodedText: String?): String? {
        if (encodedText.isNullOrEmpty()) {
            return encodedText
        }

        var result = encodedText

        // HTML 엔티티 디코딩
        HTML_ENTITIES.forEach { (entity, replacement) ->
            result = result?.replace(entity, replacement)
        }

        // 숫자 형태 HTML 엔티티 디코딩 (&#123; 형태)
        result = result?.replace("&#(\\d+);".toRegex()) { matchResult ->
            val codePoint = matchResult.groupValues[1].toIntOrNull()
            if (codePoint != null && codePoint in 1..0x10FFFF) {
                codePoint.toChar().toString()
            } else {
                matchResult.value // 변환 실패시 원본 유지
            }
        }

        // 16진수 형태 HTML 엔티티 디코딩 (&#x1F; 형태)
        result = result?.replace("&#[xX]([0-9A-Fa-f]+);".toRegex()) { matchResult ->
            val hexValue = matchResult.groupValues[1]
            runCatching {
                val codePoint = hexValue.toInt(16)
                if (codePoint in 1..0x10FFFF) {
                    codePoint.toChar().toString()
                } else {
                    matchResult.value
                }
            }.getOrElse { matchResult.value }
        }

        // 남아있는 HTML 태그 제거 (정규식 사용)
        result = result?.replace("<[^>]+>".toRegex(), "")

        // 연속된 공백을 하나로 합치고 앞뒤 공백 제거
        result = result?.replace("\\s+".toRegex(), " ")?.trim()

        // 남아있는 유니코드 이스케이프 처리
        return decodeUnicodeEscapes(result)
    }

    private fun decodeUnicodeEscapes(text: String?): String? {
        if (text.isNullOrEmpty()) {
            return text
        }
        
        return buildString {
            var i = 0
            while (i < text.length) {
                if (text.startsWith("\\u", i) && i + 5 < text.length) {
                    val hex = text.substring(i + 2, i + 6)
                    runCatching {
                        val codePoint = hex.toInt(16)
                        append(codePoint.toChar())
                        i += 6
                    }.getOrElse {
                        append(text[i])
                        i++
                    }
                } else {
                    append(text[i])
                    i++
                }
            }
        }
    }
}