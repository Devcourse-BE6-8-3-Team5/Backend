package com.back.global.ai.processor;

import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.dto.DetailQuizCreateReqDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

/**
 * 뉴스 제목과 본문을 기반 상세 퀴즈 3개를 생성하는 AI 요청 Processor 입니다.
 */
public class DetailQuizProcessor implements AiRequestProcessor<List<DetailQuizDto>> {
    private final DetailQuizCreateReqDto req;
    private final ObjectMapper objectMapper;

    public DetailQuizProcessor(DetailQuizCreateReqDto req, ObjectMapper objectMapper) {
        this.req = req;
        this.objectMapper = objectMapper;
    }

    // 뉴스 제목과 본문을 바탕으로 퀴즈 생성용 프롬프트 생성 (응답 형식을 JSON 형식으로 작성)
    @Override
    public String buildPrompt() {
        return String.format("""
                Task: 아래 뉴스 제목과 본문을 바탕으로 객관식 퀴즈 3개를 생성하세요.
                
                [퀴즈 목적]
                 - 뉴스 전체 내용을 이해했는지를 평가하기 위한 사실 기반 퀴즈를 만듭니다.
                 - 단순 상식이나 추론이 아닌, 뉴스 본문에 명확히 언급된 정보만을 바탕으로 문제를 구성하세요.
                 - 사용자가 뉴스 전체 흐름과 핵심 내용을 정확히 파악했는지를 검증하는 것이 목적입니다.
                
                 [퀴즈 형식] (반드시 지켜야 합니다):
                 각 퀴즈는 아래 항목으로 구성된 JSON 객체입니다.
                 - question: 문제 내용 (자연스럽고 명확한 문장으로 작성)
                 - option1: 선택지 1
                 - option2: 선택지 2
                 - option3: 선택지 3
                 - correctOption: 정답. 반드시 "OPTION1", "OPTION2", "OPTION3" 중 하나의 **대문자 문자열**로 작성
                
                [중복 없는 정답 배치 방법 - 반드시 따르세요]
                 퀴즈 생성 전에 다음 로직을 먼저 실행하세요:
                ```pseudo
                // STEP 1. 미리 정답 포지션을 설정한다.
                correctOptions = ["OPTION1", "OPTION2", "OPTION3"]
                shuffle(correctOptions) // 정답 위치 무작위 설정
                
                // STEP 2. 퀴즈를 3개 생성하면서, 각 퀴즈의 정답 위치를 미리 정한 대로 할당한다.
                quizzes = []
                for i in 0 to 2:
                    quiz = create_one_quiz_with_correct_option(correctOptions[i])
                    quizzes.append(quiz)
                ```
                - 정답은 미리 정해놓고, 그 정답이 해당 위치(옵션1~3)에 들어가도록 문제와 오답을 구성하세요.
                    예: correctOption = "OPTION2"인 경우 → option2에 정답, 나머지에 오답이 들어가야 합니다.

                 [출제 기준]
                 - 본문에서 명확히 언급된 사실(fact)을 기반으로 문제를 작성하세요.
                 - 추론, 상식, 감정적 해석이 필요한 문제는 절대 출제하지 마세요.
                 - 문제는 뉴스의 핵심 주제 또는 중심 내용을 다뤄야 하며, 지엽적이거나 부차적인 내용은 피해야 합니다.
                 - 오답은 본문과 유사하지만 실제로는 틀린 정보를 포함해야 하며, 사용자가 혼동할 수 있도록 설계하세요.
                
                 [응답 형식]
                 - 출력은 아래 필드들을 포함한 JSON 배열만 반환해야 하며, 그 외 추가 설명, 텍스트, 메타 정보 등은 절대 포함하지 마세요.
                
                 ```json
                 [
                   {
                     "question": "문제 내용",
                     "option1": "선택지1",
                     "option2": "선택지2",
                     "option3": "선택지3",
                     "correctOption": "OPTION1" | "OPTION2" | "OPTION3"
                   },
                   ...
                 ]
                
                input:
                {
                    "title": "%s",
                    "content": "%s"
                }
                """, req.title(), req.content());
    }

    // AI 응답을 파싱하여 DetailQuizResDto 리스트로 변환
    @Override
    public List<DetailQuizDto> parseResponse(ChatResponse response) {

        String text = response.getResult().getOutput().getText();
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException(500, "AI 응답이 비어있습니다");
        }

        List<DetailQuizDto> result;

        // JSON 형식의 응답에서 ```json ... ``` 부분을 제거하여 순수 JSON 문자열로 변환
        String cleanedJson = text.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1").trim();

        try {
            // JSON 문자열을 DetailQuizResDto 객체 리스트로 변환
            // JSON의 키 이름과 변환하려는 객체(DetailQuizResDto)의 필드 이름이 일치해야 합니다.
            result = objectMapper.readValue(
                    cleanedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DetailQuizDto.class)
            );
        } catch (Exception e) {
            throw new ServiceException(500, "AI 응답이 JSON 형식이 아닙니다. 응답: " + text);
        }

        if (result.size() != 3) {
            throw new ServiceException(500, "뉴스 하나당 3개의 퀴즈가 생성되어야 합니다. 생성된 수: " + result.size());
        }

        return result;

    }
}
