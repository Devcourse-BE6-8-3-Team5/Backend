package com.back.global.ai.processor

import com.back.domain.news.fake.dto.FakeNewsDto
import com.back.domain.news.real.dto.RealNewsDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatResponse

class FakeNewsGeneratorProcessor(
    private val realNewsDto: RealNewsDto,
    private val objectMapper: ObjectMapper
) : AiRequestProcessor<FakeNewsDto> {

    companion object {
        private val log = LoggerFactory.getLogger(FakeNewsGeneratorProcessor::class.java)
    }

    override fun buildPrompt(): String {
        val contentLength = realNewsDto.content.length

        val cleanTitle = cleanText(realNewsDto.title)
        val cleanContent = cleanText(realNewsDto.content)

        return """
            당신은 가짜 뉴스 창작 전문가입니다. **제목만을 바탕으로** 그럴듯한 가짜 뉴스를 창작하세요.
        
            ⚠️ **최우선 임무: 정확한 분량 맞추기** ⚠️
            원본 분량: ${contentLength}자 → 반드시 ${contentLength}자 ± 50자 이내로 작성!
           
            === 🎯 창작 프로세스 🎯 ===
            
            - 목표 글자수: ${contentLength}자
            
            **1단계: 내용 창작**
            - 제목 분석: "${cleanTitle}"
            - 원본 스타일 참고 (아래 참조)
            - 현실적 세부사항 포함 (날짜, 장소, 인물, 수치)
            - **매 문장마다 분량을 의식하며 작성**
            
            **2단계: 분량 검증**
            - 작성 완료 후 반드시 글자수 확인
            - ${contentLength}자와 비교하여 ±50자 이내인지 점검
            - 부족하면 세부사항 추가, 초과하면 불필요한 부분 제거

            === ⭐ 분량 맞추기 비법 ⭐ ===
            **너무 짧을 때 늘리는 방법:**
            - 구체적 날짜/시간 추가 ("지난 15일 오후 2시")
            - 정확한 장소명 추가 ("서울 강남구 테헤란로 소재")
            - 관계자 발언 인용문 추가
            - 배경 설명 1-2문장 추가
            - 관련 업계 현황 언급
            
            **너무 길 때 줄이는 방법:**
            - 불필요한 수식어 제거
            - 중복 설명 통합
            - 부가적 배경 설명 축소
            - 예상 효과 등 추측성 내용 제거
            
            === 원본 스타일 완벽 모방 ===
            **분석 대상:**
            ${cleanContent}
            
            **필수 모방 요소:**
            - 문단 수: 원본과 동일하게
            - 문장 길이: 원본 패턴 따라하기
            - 특수 기호(존재 시): ▲, ◆, -, () 등 동일 사용
            - 인용문 형식: 원본과 같은 스타일
            - 마무리 방식: 원본과 동일한 톤
            
            === 🔥 절대 금지사항 🔥 ===
            1. **분량 무시하고 창작하기** - 가장 큰 실패 요인!
            2. **제목을 content에 포함하기** - 절대 금지!
            3. **앞에 붙는 다른 제목들 포함하기** - 절대 금지!
            4. **원본 제목 그대로 복사하기** - 절대 금지!
            5. 천편일률적인 "향후 계획" 마무리
            6. 원본 내용 그대로 복사하기
            7. 비현실적이거나 과장된 내용
            8. ${contentLength}자를 크게 벗어나는 분량
            9. **\n 같은 이스케이프 문자 그대로 출력하기**
            10. **content 내부에 실제 개행문자(Enter) 사용 - JSON 파싱 실패!**
            11. **JSON 구조 중간에 끊어지기 - 파싱 불가능!**
            12. **Control character (줄바꿈, 탭 등) 원본 그대로 사용**
            
            === 💡 중요한 작성 원칙 💡 ===
            - content는 **바로 본문부터 시작**합니다
            - content는 **한 줄로 연속된 문자열**이어야 함
            - 문단 구분이 필요하면 **반드시 \n\n 텍스트로 표현**
            - 제목이나 헤더는 절대 포함하지 마세요
            - 첫 문장부터 바로 뉴스 내용으로 시작하세요
            - JSON 외부에 다른 텍스트 추가 금지
            - 코드 블록(```) 사용 금지
            - 설명이나 주석 추가 금지
            
            === JSON 출력 규칙 ===
            반드시 다음 형식으로만 응답:
            {
             "content": "정확히 ${contentLength}자 ± 50자 이내의 본문만"
            }
            
            **이스케이프 처리:**
            - 내부 따옴표: \" (백슬래시 + 따옴표)
            - **문단 구분: \n\n (백슬래시n 두 번)**
            - 백슬래시: \\ (백슬래시 + 백슬래시)
            - 작은따옴표: 그대로 ' 사용 (이스케이프 금지)
            - 한글, 영문, 숫자: 그대로 사용 (유니코드 변환 금지)
            - 특수문자, 이모지: 그대로 사용 (이스케이프 금지)
            
            
            === ✅ 최종 점검표 ✅ ===
            응답 전 반드시 확인:
            □ 글자수가 ${contentLength}자 ± 50자 이내인가?
            □ 원본과 같은 문단 구조인가?
            □ **제목이 content에 절대 포함되지 않았는가?**
            □ **첫 문장부터 바로 본문 내용인가?**
            □ 현실적이고 그럴듯한 내용인가?
            □ 원본 스타일을 잘 모방했는가?
            □ JSON 형식이 정확한가?
            
            **마지막 경고:
            - 반드시 JSON을 완성하세요: {"content": "내용"}
            - 중간에 멈추지 말고 끝까지 작성하세요!**
            
            """.trimIndent()

    }

    // AI 응답을 파싱하여 FakeNewsDto로 변환
    override fun parseResponse(response: ChatResponse): FakeNewsDto {
        val text = response.result?.output?.text?.takeIf { it.isNotBlank() }
            ?: return createFailureNotice("AI 응답이 비어있습니다")

        val cleanedJson = cleanResponse(text)
        log.debug(">>> 원본 응답\n{}", text)
        log.debug(">>> 정리 후 JSON\n{}", cleanedJson)

        return runCatching {
            val jsonNode = objectMapper.readTree(cleanedJson)
            val contentNode = jsonNode.get("content")

            val content = when {
                contentNode == null -> null
                contentNode.isNull -> null
                else -> contentNode.asText()
            }

            if (content.isNullOrBlank()) {
                createFailureNotice("AI 응답에 content가 누락되었습니다")
            } else {
                FakeNewsDto(realNewsDto.id, content)
            }
        }.getOrElse { e ->
            log.error("JSON 파싱 실패: {}", e.message)
            log.error("파싱 시도한 JSON: {}", cleanedJson)
            e.printStackTrace()
            createFailureNotice("AI 응답 파싱 실패: ${e.message}")
        }
    }

    private fun createFailureNotice(reason: String): FakeNewsDto {
        val failureContent = """
            이 뉴스는 AI 생성에 실패하여 안내문으로 대체되었습니다.
            원인: $reason
            시스템 관리자에게 문의하거나 나중에 다시 시도해 주세요.
        """.trimIndent()

        return FakeNewsDto(realNewsDto.id, failureContent)
    }


    private fun cleanResponse(text: String): String {
        return text.trim { it <= ' ' }
            .replace("(?s)```json\\s*(.*?)\\s*```".toRegex(), "$1")
            .replace("```".toRegex(), "")
            .trim { it <= ' ' }
    }

    private fun cleanText(text: String): String {
        return text.replace("\"", "'")
            .replace("%", "%%") // % -> %% 이스케이프
            .trim { it <= ' ' }
    }
}