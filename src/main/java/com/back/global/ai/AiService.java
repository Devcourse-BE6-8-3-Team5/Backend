package com.back.global.ai;

import com.back.domain.quiz.detail.dto.DetailQuizCreateReqDto;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.global.ai.processor.AiRequestProcessor;
import com.back.global.ai.processor.DetailQuizProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * 공통 AI 요청을 처리하는 메서드입니다.
     * 프롬프트 생성 및 응답 파싱 로직을 AiRequestProcessor 구현체에 위임합니다.
     *
     * @param processor 프롬프트 생성 및 응답 파싱을 담당하는 프로세서 객체
     * @param <T> 프로세서가 반환하는 타입(List<DTO> 또는 단일 DTO)
     * @return AI 응답 결과를 파싱한 객체
     */
    public <T> T process(AiRequestProcessor<T> processor) {
        String prompt = processor.buildPrompt(); // 프롬프트 생성

        ChatResponse response = chatClient.prompt(prompt)
                .call()
                .chatResponse();

        return processor.parseResponse(response); // AI 응답 파싱
    }

    // 테스트용 메서드로, 실제 서비스에서는 이용되지 않습니다.
    // AiService 개발이 끝나면 삭제할 예정입니다.
    //@PostConstruct
    public void test() {
        DetailQuizCreateReqDto req = new DetailQuizCreateReqDto(
                "알리바바, '클로드' 맞먹는 오픈 소스 AI 코딩 모델 출시",
                """
                        알리바바가 오픈 소스와 고성능, 상업적 허용이라는 3박자를 갖춘 인공지능(AI) 코딩 모델 ‘큐원3-코더(Qwen3-Coder)’를 공개했다. AI 코딩 모델 시장의 판도를 뒤흔들만하다는 평이다.
                        알리바바는 23일(현지시간) 복잡한 코딩 작업을 수행할 수 있는 오픈 소스 대형언어모델(LLM) ‘큐원3-코더-480B-A35B-인스트럭트’를 출시했다.
                        이는 불과 며칠 전, 비추론(non-reasoning) 언어모델 중 최고 성능을 자랑하는 ‘큐원3-235B-A22B-2507’을 공개한 데 이어 나온 것이다.
                        큐원3-코더는 소프트웨어 개발 지원에 특화한 LLM으로, 다단계의 복잡한 코딩 워크플로우를 처리하고 실제 작동하는 애플리케이션을 몇초에서 몇분 만에 완성할 수 있다.
                        특히 엔지니어링 자동화, 도구 연동, 코드 생성 및 수정, SQL 프로그래밍 등 다양한 작업에서 최고 수준의 성능을 기록하며, '클로드 소네트 4'나 'GPT-4.1' 같은 상용 모델과 직접 경쟁할 수준이라는 평가를 받는다.
                        또 오픈 소스로 공개, 누구나 자유롭게 다운로드, 수정, 배포, 상업적 활용이 가능하다. 사용자는 허깅페이스, 깃허브, 큐원 챗, 큐원 API 등 다양한 채널을 통해 모델에 접근할 수 있으며, 로컬에서 직접 구동하거나 API를 통해 활용할 수도 있다.
                        전문가 혼합(MoE) 방식으로 총 4800억개의 매개변수를 갖췄다. 쿼리마다 350억 개의 매개변수와 160개 전문가 중 8개만 활성화되는 구조를 통해 효율성을 높였다.
                        25만6000 토큰의 컨텍스트 길이를 지원하며, RoPE 확장 기법인 YaRN을 적용해 최대 100만 토큰까지도 처리할 수 있다. 이는 대규모 코드베이스나 방대한 기술 문서의 일괄 이해 및 조작에 적합한 구조다.
                        고도화된 사후 훈련(post-training) 기술도 적용했다고 밝혔다. ▲실행 기반 강화 학습(Code RL) ▲장기적 계획 수립 능력을 기르는 강화 학습(Long-Horizon Agent RL) 등으로, 실제 소프트웨어 개발 환경을 시뮬레이션한 2만개 환경에서 모델을 훈련했다.
                        프로그래밍 에이전트 평가 'SWE-벤치'에서 정답률 67.0%(표준), 69.6%(500턴 테스트)를 기록했다.
                        이는 GPT-4.1(54.6%)과 '제미나이 2.5 프로 프리뷰(49.0%)'를 크게 웃도는 수치다. '클로드 소네트 4(70.4%)'에 근접한 성능이다.
                        또 멀티언어 코딩, 브라우저 기반 작업, 도구 활용 등 다양한 에이전트 과제에서도 경쟁력을 입증했다.
                        일리바바는 이날 개발자 터미널용 오픈소스 코딩 도구 ‘큐원 코드(Qwen Code)’도 공개했다.
                        이는 앤트로픽의 '클로드 코드'와 비슷한 것으로, 명령줄 터미널에서 직접 실행되며 구조화된 프롬프트 및 함수 호출 기능을 통해 큐원3-코더를 다양한 개발 워크플로우에 통합할 수 있게 돕는다.
                        전문가와 실무 개발자의 반응도 뜨겁다.
                        트위터 공동 창업자로 유명한 잭 도시는 자신의 AI 프레임워크 ‘구스(Goose)’와 큐원3-코더를 결합해 테스트한 뒤 “와우(Wow)”라는 반응을 보였다.
                        미국의 AI 연구자 세바스찬 라슈카는 X(트위터)를 통해 “지금까지 나온 코딩 특화 모델 중 최고”라며 “범용 모델보다 특화된 모델이 승리한다”라고 평가했다. AI 엔지니어 울프람 레이븐울프도 “현재 사용 가능한 모델 중 가장 뛰어나다”라고 극찬했다.
                        """
        );

        // 프롬프트 생성 및 응답 파싱을 담당하는 Processor 객체 생성
        DetailQuizProcessor processor = new DetailQuizProcessor(req, objectMapper);
        // AI 호출 후 결과 출력
        List<DetailQuizDto> resDtoList =  process(processor);
        for( DetailQuizDto resDto : resDtoList) {
            System.out.println(resDto);
        }
    }
}
