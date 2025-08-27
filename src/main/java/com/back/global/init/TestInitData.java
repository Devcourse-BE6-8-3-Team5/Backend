package com.back.global.init;


import com.back.domain.member.member.service.MemberService;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.fake.service.FakeNewsService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.NewsDataService;
import com.back.domain.quiz.daily.service.DailyQuizService;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.entity.Option;
import com.back.domain.quiz.detail.service.DetailQuizService;
import com.back.domain.quiz.fact.service.FactQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@Profile("test")
@RequiredArgsConstructor
public class TestInitData {
    @Autowired
    @Lazy
    private TestInitData self;
    private final MemberService memberService;
    private final NewsDataService newsDataService;
    private final FakeNewsService fakeNewsService;
    private final DetailQuizService detailQuizService;
    private final FactQuizService factQuizService;
    private final DailyQuizService dailyQuizService;

    @Bean
    ApplicationRunner testInitDataApplicationRunner() {
        return args -> {
            self.memberInit();
            self.newsInit();
            self.fakeNewsInit();
            self.detailQuizInit();
            self.factQuizInit();
            self.dailyQuizInit();
        };
    }

    @Transactional
    public void memberInit() {
        if (memberService.count() > 0) {
            return;
        }

        memberService.join("system","12345678", "system@gmail.com");
        memberService.join("admin","12345678", "admin@gmail.com");
        memberService.join("user1","12345678", "user1@gmail.com");
        memberService.join("user2","12345678", "user2@gmail.com");
        memberService.join("user3","12345678", "user3@gmail.com");
        memberService.join("user4","12345678", "user4@gmail.com");


    }

    @Transactional
    public void newsInit() {
        // 뉴스가 이미 있으면 초기화 생략
        if (newsDataService.count() > 0) {
            return;
        }

        // 뉴스 1: 경제 카테고리
        RealNewsDto news1 = RealNewsDto.of(
                1L,
                "소비쿠폰 지급+여름 휴가철 시작…내수경기 반등할까",
                "7월 첫째 주 신용카드 이용금액 작년 대비 12.6% 증가 교육 서비스 이용 9.9% 증가 등 교육·보건이 증가 견인 28일 서울의 한 이마트에 민생회복 소비쿠폰 관련 안내문이 내걸려 있다. 이마트에 따르면 전국 156개 이마트·트레이더스 점포에 입점한 2천600여개 임대매장 중에서 민생회복 소비쿠폰 사용이 가능한 매장은 960여개로 전체의 37% 수준이다. 연합뉴스 정부가 '민생회복 소비쿠폰'을 지급 중인 가운데 본격적인 여름 휴가철에 접어들면서 내수경기 반등에 대한 기대감이 번지고 있다. 29일 통계청 속보성 지표 '나우캐스트'에 따르면 7월 첫째 주(6월 28일∼7월 4일) 신용카드 이용금액은 작년 동기보다 12.6% 증가했다. 7월 둘째 주(7월 5∼11일)도 3.7% 증가하며 작년 대비 상승 흐름을 이어갔다. 업종별로 보면 내수와 밀접한 업종보다는 교육, 보건 등이 증가세를 이끌었다. 7월 둘째 주 교육 서비스 이용금액이 작년 동기보다 9.9% 크게 늘었고 보건 부문도 4.9% 증가했다. 반면 숙박서비스는 2.4% 감소했고, 음식·음료 서비스도 4.2% 줄었다. 식료품과 음료 결제액도 2.0% 감소했다. 정부는 지난 21일 지급을 시작한 소비쿠폰이 '내수 마중물' 역할을 할 것으로 기대하고 있다. '7말 8초'(7월 말부터 8월 초) 여름 휴가철과 맞물려 소비 진작 효과가 커질 가능성도 있다. 정부 관계자는 \"신용카드 주간 결제액은 변동성이 크다\"며 \"최근 일부 지표에서 소비 개선 흐름은 있다\"고 했다. 기획재정부 경제동향을 보면 지난달 카드 국내 승인액은 작년 동월보다 4.5% 증가했고, 한국을 찾은 중국인 관광객 수는 28.8% 늘었다. 소비심리도 개선 흐름을 보이는 상황이다. 이번 달 소비자심리지수(CCSI)는 110.8로, 지난달보다 2.1포인트(p) 올라 2021년 6월(111.1) 이후 4년여 만에 최고치를 경신했다. 지난 3월 93.4, 4월 93.8, 5월 101.8, 6월 108.7에 이어 7월까지 넉 달 연속 상승세다. 다만 휴가철 해외여행 수요가 증가하면서 국내 소비가 해외로 분산될 가능성도 있다. 소비쿠폰도 사용처를 일부 제한하기는 했지만 사교육비와 담배 '사재기' 등에 쓰이고 있다는 지적도 나오고 있어, 실질적인 내수 진작 효과는 향후 지표를 통해 판단될 전망이다.",
                "정부가 '민생회복 소비쿠폰'을 지급 중인 가운데 본격적인 여름 휴가철에 접어들면서 내수경기 반등에... '7말 8초'(7월 말부터 8월 초) 여름 휴가철과 맞물려 소비 진작 효과가 커질 가능성도 있다. 정부 관계자는...",
                "https://n.news.naver.com/mnews/article/088/0000961622?sid=101",
                "https://imgnews.pstatic.net/image/088/2025/07/29/0000961622_001_20250729133508809.jpg?type=w860",
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(3),
                "매일신문",
                "정은빈 기자",
                "https://www.imaeil.com/page/view/2025072913253167202",
                NewsCategory.ECONOMY
        );

        // 뉴스 2: 생활 카테고리
        RealNewsDto news2 = RealNewsDto.of(
                2L,
                "여름 제철 간식 감자·찰옥수수 드세요",
                "[서울=뉴시스] 29일 서울 서초구 농협유통 하나로마트 양재점에서 모델들이 여름 제철 간식인 감자와 찰옥수수를 소개하고 있다. (사진=농협유통 제공) 2025.07.29. photo@newsis.com *재판매 및 DB 금지",
                "29일 서울 서초구 농협유통 하나로마트 양재점에서 모델들이 여름 제철 간식인 감자와 찰옥수수를 소개하고 있다. (사진=농협유통 제공) 2025.07.29. photo@newsis.com *재판매 및 DB 금지",
                "https://n.news.naver.com/mnews/article/003/0013391031?sid=101",
                "https://imgnews.pstatic.net/image/003/2025/07/29/NISI20250729_0020908209_web_20250729134833_20250729134922533.jpg?type=w860",
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(2),
                "뉴시스",
                "류현주 기자",
                "https://www.newsis.com/view/NISI20250729_0020908209",
                NewsCategory.CULTURE
        );

        // 뉴스 3: 여행/레저 카테고리 - new 키워드 제거
        RealNewsDto news3 = RealNewsDto.of(
                3L,
                "경제계 \"상법·노조법 개정안 국회 급물살, 우려 넘어 참담\"",
                "한경협·대한상의 등 경제8단체 공동입장문 \"국회, 연이은 규제 입법…기업 극도 혼란\" \"개정안 국익 관점서 신중하게 재검토 호소\" [서울=뉴시스] 김금보 기자 = 김주영 국회 환경노동위원회 소위원장이 28일 서울 여의도 국회에서 열린 '노란봉투법(노동조합 및 노동관계조정법 2·3조 개정안)' 관련 제1차 고용노동법안심사소위원회에서 의사봉을 두드리고 있다. 2025.07.28. kgb@newsis.com[서울=뉴시스]이현주 기자 = 경제계는 국회에서 더 강한 상법 및 노란봉투법(노동조합 및 노동관계조정법 제2·3조 개정안) 처리가 급물살을 타는 데 대해 \"깊은 우려를 넘어 참담한 심정을 금할 수 없다\"고 반발했다. 경제8단체는 29일 '내우외환 한국경제, 국회의 현명한 판단한 바란다'는 제목의 공동 입장문을 통해 이같이 밝혔다. 이번 입장문에는 한국경제인협회, 대한상공회의소, 한국경영자총협회, 한국무역협회, 중소기업중앙회, 한국중견기업연합회, 한국상장회사협의회, 코스닥협회 등 8개 단체가 참여했다. 이들 단체는 \"이사의 충실의무 확대 등을 담은 상법 개정안이 공포된 지 채 1주일도 지나지 않아 추가 상법 개정안이 법안소위에서 처리됐고, 노조법 개정안 역시 하루 만에 법안소위와 전체 회의를 연달아 통과했다\"고 지적했다. 그러면서 \"정부와 국회, 기업이 위기 극복을 위해 하나로 뭉쳐야 하는 중차대한 시점에 국회가 기업활동을 옥죄는 규제 입법을 연이어 쏟아내는 것은 기업들에게 극도의 혼란을 초래할 수 있다\"며 \"관세 협상의 결과가 불투명한 상황에서 자승자박하는 것은 아닌지 안타깝다\"고 전했다. 경제8단체는 상법 추가 개정에 대해 \"사업재편 반대, 주요 자산 매각 등 해외 투기자본의 무리한 요구로 이어져 주력산업의 구조조정과 새로운 성장동력 확충을 어렵게 할 수 있다\"고 우려했다. 노조법 개정안 역시 \"사용자 범위가 확대되고, 기업 고유의 경영활동까지도 쟁의 대상에 포함돼 파업 만능주의를 조장하고 노사관계 안정성도 훼손되는 등 심각한 부작용이 우려된다\"고 짚었다. 이들 단체는 \"새 정부가 성장 중심의 경제정책에 대한 의지를 밝힌 만큼 위기 극복을 위해 정부와 국회, 기업이 하나가 되어 모든 역량을 총동원해야 할 때\"라며 \"꺼져가는 성장동력을 재점화하고 양질의 일자리 창출을 위해 기업들이 전력을 다할 수 있는 환경을 조성하는데 국회가 나서주기를 바란다\"고 요청했다. 이어 \"국회는 지금이라도 우리 기업이 처한 어려움과 절박한 호소를 외면하지 말길 바란다\"며 \"기업들이 외부의 거센 파고를 넘는 데 전념할 수 있도록 부디 불필요한 규제를 거두고 개정안들을 철저히 국익 관점에서 신중하게 재검토해달라\"고 호소했다．",
                "경제계는 국회에서 더 강한 상법 및 노란봉투법(노동조합 및 노동관계조정법 제2·3조 개정안) 처리가 급물살을 타는 데 대해 \"깊은 우려를 넘어 참담한 심정을 금할 수 없다\"고 반발했다. 경제8단체는 29일 '내우외환...",
                "https://n.news.naver.com/mnews/article/003/0013389987?sid=101",
                "https://imgnews.pstatic.net/image/003/2025/07/29/NISI20250728_0020906501_web_20250728105814_20250729090316531.jpg?type=w860",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(3),
                "연합뉴스",
                "강태현 기자",
                "https://www.yna.co.kr/view/AKR20250729103800062?input=1195m",
                NewsCategory.CULTURE
        );
        RealNewsDto news4 = RealNewsDto.of(
                4L,
                "무더위 날리는 풀 파티·불꽃 쇼…하이원리조트 여름 이벤트",
                "하이원 워터월드 전경 [강원랜드 제공. 재판매 및 DB 금지] (정선=연합뉴스) 강태현 기자 = 강원랜드가 운영하는 하이원리조트에서 여름 휴가철을 맞아 다양한 여름철 이벤트를 선보인다. 하이원 워터월드 야외 파도 풀 포세이돈 웨이브에서는 내달 10일까지 'DJ 풀 파티'가 열린다. 풀 파티는 매일 오후 2시부터 시작되며 청정 계곡수에서 음악과 물살이 어우러져 짜릿함을 선사한다. 박명수, DJ소다 등 인기 DJ들의 EDM(일렉트로닉 댄스 뮤직)·힙합 공연과 3ｍ 인공파도, 물대포 워터캐논이 더해져 흥겨움은 배가 된다. 해가 지면 그랜드호텔 잔디광장에서 화려한 '하이원 레이저 불꽃 쇼'가 펼쳐진다. 올해는 우주를 콘셉트로 DJ 공연·레이저·불꽃놀이가 어우러진 40분간의 야간 공연이 내달 1·2·9일 열린다. 8월 15일 광복절 80주년 기념 드론 불꽃 쇼도 진행된다. DJ 풀 파티 [강원랜드 제공. 재판매 및 DB 금지] 하이원 곳곳에서는 제설기로 만드는 물 폭탄 축제 '미니 워터밤'과 하이원 아티스트들의 버스킹 공연, 별빛 산책과 요가 명상 등 자연 속 웰니스 프로그램도 마련돼 있다. 올여름 성수기 시즌 마운틴카페테리아에서 즐길 수 있는 '산상 바비큐'와 하이원 대표 메뉴 '오리엔 냉짬뽕' 등 하계 시즌을 겨냥한 다양한 식음료도 만나볼 수 있다. 자세한 이벤트 내용은 하이원리조트 공식 홈페이지에서 확인할 수 있다. 이민호 강원랜드 마케팅기획실장은 \"산 좋고 물 좋고 즐길 거리와 먹거리까지 풍성한 하이원리조트에서 온종일 알찬 시간 보내고 무더위와 스트레스까지 싹 날려버리길 바란다\"고 말했다. 하이원리조트 불꽃놀이 [강원랜드 제공. 재판매 및 DB 금지] taetae@yna.co.kr",
                "강원랜드가 운영하는 하이원리조트에서 여름 휴가철을 맞아 다양한 여름철 이벤트를 선보인다. 하이원 워터월드 야외 파도 풀 포세이돈 웨이브에서는 내달 10일까지 'DJ 풀 파티'가 열린다. 풀 파티는 매일 오후 2시부터...",
                "https://n.news.naver.com/mnews/article/001/0015536299?sid=102",
                "https://imgnews.pstatic.net/image/001/2025/07/29/AKR20250729103800062_01_i_P4_20250729141024634.jpg?type=w860",
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(3),
                "뉴시스",
                "이현주 기자",
                "https://www.newsis.com/view/NISX20250729_0003269924",
                NewsCategory.ECONOMY
        );


        RealNewsDto news5 = RealNewsDto.of(
                5L,
                "경제8단체, 상법·노조법 개정에 \"국회 신중히 재검토해달라\"",
                "한경협·대한상의 등 \"韓경제 미래 결정될 분수령서 극도혼란 초래\" 상법개정안 여당 주도 법사소위 통과 (서울=연합뉴스) 김주성 기자 = 더불어민주당 장경태 의원 등 여당 법제사법위원회 위원들이 28일 국회 법사위 법안심사소위에서 집중투표제를 의무화하고 감사위원 분리 선출을 확대하는 내용의 상법 개정안을 여당 주도로 통과시킨 후 취재진에게 내용을 설명하고 있다. 왼쪽부터 더불어민주당 박균택·김용민·장경태·이성윤 의원. 2025.7.28 utzza@yna.co.kr (서울=연합뉴스) 김보경 기자 = 재계는 한국 경제의 명운을 가를 미국과의 관세 협상 마감이 임박한 가운데 기업 경영에 부담을 줄 수 있는 상법 및 노동조합법 2·3조 개정안 논의가 국회에서 빠르게 진행되는 것과 관련, 큰 우려를 표하며 개정안 재검토를 호소했다. 한국경제인협회와 대한상공회의소, 한국경영자총협회, 한국무역협회, 중소기업중앙회, 한국중견기업연합회, 한국상장회사협의회, 코스닥협회 등 경제8단체는 29일 배포한 공동 입장문에서 \"엄중한 경제 상황에도 상법 및 노조법 개정안이 국회에서 급물살을 타는 것에 대해 깊은 우려를 넘어 참담한 심정을 금할 수 없다\"고 밝혔다. 경제8단체는 올해 우리 경제가 0.8% 성장에 그칠 것으로 전망되는 등 초저성장 국면이 지속되는 상황에서 며칠 앞으로 다가온 대미 통상 협상 결과가 한국 경제의 미래를 결정할 중요한 분수령이 될 것이라고 강조했다. 이들 단체는 \"우리 기업의 평균 영업이익률이 5% 내외인 상황에서 한미 관세 협상이 난항을 겪는다면 미국으로 수출하는 길이 사실상 막히게 된다\"며 \"이는 우리나라 최대 수출 시장을 잃는 것이고, 경제 정책 및 기업 경영 전략을 새롭게 수립해야 할 중대한 상황을 맞이하게 될 것\"이라고 우려했다. 노조법 2·3조 개정안 상정에 반발해 퇴장하는 국민의힘 환노위원들 (서울=연합뉴스) 황광모 기자 = 국회 환경노동위원회 국민의힘 의원들이 28일 윤석열 전 대통령이 거부권(재의요구권)을 행사했던 '노란봉투법'(노조법 2·3조 개정안)이 국회 환경노동위원회에 상정되자 강한 유감을 표현한 뒤 회의장에서 퇴장하고 있다. 2025.7.28 hkmpooh@yna.co.kr 이런 위기 속 기업 경영에 부담을 줄 수 있는 상법 개정안과 이른바 '노란봉투법'(노조법 개정안)이 국회에서 통과될 경우 기업들은 큰 혼란에 빠질 것이라고 경제8단체는 경고했다. 이사의 충실의무 확대 등을 담은 상법 개정안은 지난 22일 공포된 후 1주일도 안 돼 추가 상법 개정안이 현재 국회 법제사법위원회 법안소위에서 처리됐고, 노조법 개정안이 법안소위와 전체 회의를 연달아 통과한 바 있다. 이들 단체는 \"정부와 국회, 기업이 위기 극복을 위해 하나로 뭉쳐야 하는 중차대한 시점에 국회가 기업활동을 옥죄는 규제 입법을 연이어 쏟아내는 것은 기업에 극도의 혼란을 초래할 수 있다\"며 \"관세 협상의 결과가 불투명한 상황에서 자승자박하는 것은 아닌지 안타깝다\"고 지적했다. 그러면서 \"상법 추가 개정은 사업재편 반대, 주요 자산 매각 등 해외 투기자본의 무리한 요구로 이어져 주력산업의 구조조정과 새로운 성장동력 확충을 어렵게 할 수 있다\"며 \"노조법 개정안도 사용자 범위가 확대되고, 기업 고유의 경영활동까지도 쟁의 대상에 포함돼 파업 만능주의를 조장하고 노사관계 안정성도 훼손되는 등 심각한 부작용이 우려된다\"고 덧붙였다. 이들 단체는 \"새 정부가 성장 중심 경제정책의 의지를 밝힌 만큼 위기 극복을 위해 정부와 국회, 기업이 하나가 되어 모든 역량을 총동원해야 한다\"며 \"꺼져가는 성장동력을 재점화하고 양질의 일자리 창출을 위해 기업들이 전력을 다할 수 있는 환경을 조성하는데 국회가 나서주기를 바란다\"고 호소했다. 이어 \"국회는 지금이라도 우리 기업이 처한 어려움과 절박한 호소를 외면하지 말길 바란다\"며 \"개정안들을 철저히 국익 관점에서 신중하게 재검토해 주기를 간곡히 호소한다\"고 덧붙였다. vivid@yna.co.kr",
                "한경협·대한상의 등 \"韓경제 미래 결정될 분수령서 극도혼란 초래\" 재계는 한국 경제의 명운을 가를 미국과의 관세 협상 마감이 임박한 가운데 기업 경영에 부담을 줄 수 있는 상법 및 노동조합법 2·3조 개정안 논의가...",
                "https://n.news.naver.com/mnews/article/001/0015535245?sid=101",
                "https://imgnews.pstatic.net/image/001/2025/07/29/PYH2025072813430001300_P4_20250729093815961.jpg?type=w860",
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(2),
                "연합뉴스",
                "김보경 기자",
                "https://www.yna.co.kr/view/AKR20250729042500003?input=1195m",
                NewsCategory.ECONOMY
        );
        RealNewsDto news6 = RealNewsDto.of(
                6L,
                "인도 최대 IT 서비스 TCS 1만2200명 감원 계획",
                "[서울=뉴시스]이재준 기자 = 인도 최대 정보기술(IT) 서비스업체 TCS(타타 컨설턴시 서비스)는 2025/26 회계연도(2025년 4월~2026년 3월)에 직원 1만2200명을 감축한다고 PTI 통신과 마켓워치 등이 29일 보도했다. 매체는 TCS 발표를 인용해 1년 동안 전체 직원 61만3000여명의 2%에 상당하는 감원을 실시하며 구조조정 대상이 주로 중간과 간부 관리직이라고 전했다. 수요 전망이 불투명한 가운데 TCS는 인공지능(AI) 등 기술을 도입해 신규 분야에 진출하기 위한 재원을 확보하는 차원에서 대대적인 감원에 나선다고 매체는 분석했다. TCS는 성명을 통해 \"이번 인력 감축으로 고객에 제공하는 서비스가 차질을 빚지 않도록 충분한 대책을 강구할 방침\"이라고 밝혔다. 인도 IT 업계는 수요 침체, 지속적인 인플레, 미국 고관세 정책을 배경으로 고객이 필요 불가결하지 않은 서비스 지출을 억제하는 상황에 직면했다. 앞서 K 크리티바산 TCS 최고경영자(CEO)는 이달 들어 고객의 의사결정과 프로젝트 개시가 지연되는 현상을 보이고 있다고 지적했다. 2830억 달러(약 393조5115억원) 규모인 인도 IT 서비스 산업은 1분기(4~6월)에 신규 채용을 크게 줄였다. TCS를 비롯한 상위 6대 기업(Infosys·HCLTech·Wipro·Tech Mahindra·LTIMindtree)의 순채용 수는 3847명로 전기 1만 3935명보다 72% 급감했다.",
                "인도 최대 정보기술(IT) 서비스업체 TCS(타타 컨설턴시 서비스)는 2025/26 회계연도(2025년 4월~2026년 3월)... 인도 IT 업계는 수요 침체, 지속적인 인플레, 미국 고관세 정책을 배경으로 고객이 필요 불가결하지...",
                "https://n.news.naver.com/mnews/article/003/0013390843?sid=104",
                "https://imgnews.pstatic.net/image/003/2025/07/29/NISI20181111_0014639173_web_20181111100557_20250729121621751.jpg?type=w860",
                LocalDateTime.now().minusDays(4),
                LocalDateTime.now().minusDays(2),
                "뉴시스",
                "이재준 기자",
                "https://www.newsis.com/view/NISX20250729_0003270456",
                NewsCategory.IT
        );
        RealNewsDto news7 = RealNewsDto.of(
                7L,
                "LG CNS, K뱅킹 시스템 수출…동남아 금융 IT 시장 공략",
                "인도네시아 시나르마스 은행 카드 시스템 사업 수주 LG CNS 로고 [LG CNS 제공. 연합뉴스 자료 사진. 재판매 및 DB 금지] (서울=연합뉴스) 김경희 기자 = LC GNS는 28일 인도네시아 상업은행인 시나르마스 은행의 차세대 카드 시스템 구축 사업을 수주, 동남아 금융 정보기술(IT) 시장 공략에 본격 나선다고 밝혔다. 이번 사업은 LG CNS가 지난해 시나르마스 그룹과 설립한 합작 법인 LG 시나르마스 출범 이후 금융 분야에서 수주한 첫번째 프로젝트이자, 해외 현지 금융사의 차세대 시스템 구축 첫 사례다. 사업 기간은 총 6년으로, 1년간 시스템을 구축한 뒤 5년간 유지보수 서비스를 제공한다. 시나르마스 은행은 직불카드와 신용카드 관리 시스템을 각각 운영 중이다. 이로 인해 시스템 운영 및 관리에 어려움이 있고, 유지 보수 비용이 많이 발생한다는 지적이 나온다. LG CNS는 자체 카드 비즈니스 설루션 카드퍼펙트를 활용, 시나르마스 은행의 직불카드 관리 시스템과 신용카드 관리 시스템을 통합한 차세대 카드 시스템을 구축할 방침이다. LG CNS는 이번 사업을 교두보 삼아 인도네시아를 포함한 베트남, 캄보디아 등 동남아시아권으로 사업을 확대할 방침이다. 동남아권 은행을 대상으로 카드퍼펙트 설루션 설명회를 지속 진행 중이며, 글로벌 은행으로부터 설루션 도입 문의 역시 증가 추세라고 회사측은 전했다. 국내의 경우 2016년부터 다수 금융사가 해당 시스템을 운영 중이며, NH농협은행, 신한은행, 신한카드, KB금융그룹 등의 인공지능 전환(AX) 사업도 담당 중이다. 회사 관계자는 \"국내를 넘어 해외 현지 금융사의 차세대 카드 시스템 구축 사업을 수주한 것은 의미가 크다\"며 \"국내에서 일찍이 검증받은 설루션으로 글로벌 고객들에게도 차별적 가치를 제공하겠다\"고 강조했다. kyunghee@yna.co.kr",
                "인도네시아 시나르마스 은행 카드 시스템 사업 수주 LC GNS는 28일 인도네시아 상업은행인 시나르마스 은행의 차세대 카드 시스템 구축 사업을 수주, 동남아 금융 정보기술(IT) 시장 공략에 본격 나선다고 밝혔다. 이번...",
                "https://n.news.naver.com/mnews/article/001/0015533231?sid=101",
                "https://imgnews.pstatic.net/image/001/2025/07/28/PCM20241214000158017_P4_20250728100510353.jpg?type=w860",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                "연합뉴스",
                "김경희 기자",
                "https://www.yna.co.kr/view/AKR20250728046500017?input=1195m",
                NewsCategory.IT
        );
        RealNewsDto news8 = RealNewsDto.of(
                8L,
                "콜드플레이 美공연장 '키스캠'에 메시 부부 포착…관객들 환호",
                "최근 콜드플레이 콘서트장의 '키스캠'에 리오넬 메시 부부가 포착되어 화제가 되었다. 메시는 콘서트 후 인스타그램에 사진을 게시했고, 18시간 만에 680만 개 이상의 '좋아요'를 받았다. 한편, 콜드플레이 콘서트의 키스캠은 커플을 비추는 이벤트로 인기를 끌고 있으며, 최근 불륜 커플이 포착되어 화제가 되기도 했다.",
                "당신과 당신의 아내는 정말 좋아 보인다\"는 가사를 붙여 짧게 노래한 뒤 \"오늘 우리 밴드 공연을 보러... 메시는 이 공연이 끝난 뒤 인스타그램에 아내, 세 아들과 함께 콘서트장에서 찍은 사진들을 올렸다. 이 게시물은...",
                "https://n.news.naver.com/mnews/article/001/0015535197?sid=103",
                "https://imgnews.pstatic.net/image/001/2025/07/29/AKR20250729038500075_01_i_P4_20250729092514790.jpg?type=w860",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                "연합뉴스",
                "임미나 기자",
                "https://www.yna.co.kr/view/AKR20250729038500075?input=1195m",
                NewsCategory.CULTURE
        );

        List<RealNewsDto> newsList = List.of(news1, news2, news3, news4, news5, news6, news7, news8);
        List<RealNewsDto> savedNewsList = newsDataService.saveAllRealNews(newsList);

        if (!savedNewsList.isEmpty()) {
            newsDataService.setTodayNews(savedNewsList.get(1).id());
        }
    }

    @Transactional
    public void fakeNewsInit() {
        // 페이크뉴스가 이미 있으면 초기화 생략
        if (fakeNewsService.count() > 0) {
            return;
        }

        // 실제 뉴스가 있는지 확인
        if (newsDataService.count() == 0) {
            throw new IllegalStateException("실제 뉴스가 먼저 생성되어야 합니다.");
        }

        FakeNewsDto fakeNews1 = FakeNewsDto.of(
                1L,
                "정부가 민생회복 소비쿠폰 지급 예산을 당초 계획보다 3배 증액하여, 국민 1인당 30만원을 지급하는 방안을 확정했다고 밝혔다. 이번 대폭 증액은 침체된 내수 경기를 활성화하고, 소비 심리를 끌어올리기 위한 정부의 강력한 의지를 반영한 조치로 풀이된다. 관계 당국은 이번 소비쿠폰 지급이 여름 휴가철과 맞물려 시너지 효과를 내어 경제 전반에 긍정적인 파급 효과를 가져올 것으로 기대하고 있다. 자세한 지급 시기와 신청 방법은 추후 공지될 예정이다."
        );

        FakeNewsDto fakeNews2 = FakeNewsDto.of(
                2L,
                "최근 국내 농가에서 개발된 신품종 감자와 찰옥수수가 여름철 건강 간식으로 큰 인기를 끌고 있다. 특히 이 품종들은 일반 감자, 옥수수보다 비타민 함량이 2배 이상 높고, 식이섬유가 풍부하여 다이어트와 장 건강에도 도움을 준다고 알려졌다. 유명 셰프들도 이 신품종을 활용한 다양한 레시피를 선보이며 소비자들의 관심을 한몸에 받고 있다. 농림축산식품부는 해당 품종의 전국적인 보급 확대를 위해 재배 농가에 대한 지원을 강화할 방침이다."
        );

        FakeNewsDto fakeNews3 = FakeNewsDto.of(
                3L,
                "국회에서 논의 중이던 상법 및 노동조합법 개정안이 경제계의 거센 반발에도 불구하고 초고속으로 통과될 전망이다. 이는 정부와 여당이 기업 활동을 옥죄는 규제를 대폭 강화하여, 국내 기업들의 해외 이탈을 가속화하고 국가 경쟁력을 심각하게 저해할 수 있다는 경제 단체들의 경고에도 불구하고 추진되고 있는 것으로 알려졌다. 재계는 이번 개정안 통과가 투자 위축과 일자리 감소로 이어질 것이라며 강하게 비판하고 있어, 향후 경제에 미칠 파장이 클 것으로 예상된다."
        );

        FakeNewsDto fakeNews4 = FakeNewsDto.of(
                4L,
                "하이원리조트가 여름철 이벤트를 대폭 축소하고, 워터월드 폐장 및 불꽃 쇼 전면 취소를 발표했다. 리조트 측은 예상보다 저조한 방문객 수와 인건비 부담을 이유로 들며, 기존에 계획했던 DJ 풀 파티, 미니 워터밤 등의 행사도 모두 중단한다고 밝혔다. 이로 인해 여름 휴가를 계획했던 많은 방문객들이 큰 혼란을 겪고 있으며, 인근 지역 상권에도 부정적인 영향이 예상된다. 하이원리조트는 추후 새로운 운영 계획을 발표할 예정이다."
        );

        FakeNewsDto fakeNews5 = FakeNewsDto.of(
                5L,
                "경제8단체가 한국 경제의 활성화를 위해 정부에 상법 및 노동조합법 개정안의 즉각적인 통과를 강력히 촉구하고 나섰다. 이들은 공동 성명을 통해 현재의 경기 침체와 글로벌 경제 위기를 극복하기 위해서는 기업 활동의 자유를 보장하고 노동 시장의 유연성을 확대하는 것이 필수적이라고 강조했다. 경제계는 또한 현재 진행 중인 미국과의 관세 협상에 대해서도 정부가 적극적인 자세로 임하여 기업들의 수출길을 열어줄 것을 요청했다."
        );

        FakeNewsDto fakeNews6 = FakeNewsDto.of(
                6L,
                "인도 최대 IT 서비스 기업 TCS가 대규모 신규 채용 계획을 발표하며 전 세계 IT 업계의 주목을 받고 있다. TCS는 향후 1년간 인공지능(AI) 및 클라우드 컴퓨팅 분야의 전문가 5만 명을 추가로 고용할 예정이라고 밝혔다. 이는 급증하는 디지털 전환 수요에 대응하고, 신기술 분야에서의 글로벌 경쟁력을 강화하기 위한 전략의 일환이다. 이번 채용 발표는 최근 IT 업계 전반의 인력 감축 분위기와는 대조적이어서 더욱 큰 관심을 끌고 있다."
        );

        FakeNewsDto fakeNews7 = FakeNewsDto.of(
                7L,
                "LG CNS가 K뱅킹 시스템 수출에 실패하고 동남아 금융 IT 시장 진출 계획을 전면 철회했다. 인도네시아 시나르마스 은행과의 차세대 카드 시스템 구축 사업은 기술적인 문제와 현지 시장 이해 부족으로 결국 무산되었다. 이번 사업 실패로 LG CNS는 상당한 재정적 손실을 입었으며, 동남아 시장에서의 신뢰도에도 큰 타격을 받게 되었다. 회사 관계자는 이번 실패를 계기로 해외 사업 전략을 전면 재검토할 예정이라고 밝혔다."
        );

        // 페이크뉴스 리스트 생성 및 저장
        List<FakeNewsDto> fakeNewsList = List.of(fakeNews1, fakeNews2, fakeNews3, fakeNews4, fakeNews5, fakeNews6, fakeNews7);
        fakeNewsService.saveAllFakeNews(fakeNewsList);
    }

    @Transactional
    public void detailQuizInit() {
        // 퀴즈가 이미 있으면 초기화 생략
        if (detailQuizService.count() > 0) {
            return;
        }

        // 뉴스 1 상세 퀴즈
        DetailQuizDto quiz1 = new DetailQuizDto("7월 첫째 주 신용카드 이용금액은 작년 동기 대비 얼마나 증가했습니까?", "9.9%", "12.6%", "4.5%", Option.OPTION2);
        DetailQuizDto quiz2 = new DetailQuizDto("7월 둘째 주 신용카드 이용 증가를 견인한 업종은 무엇입니까?", "숙박 서비스 및 음식·음료 서비스", "식료품 및 음료", "교육 서비스 및 보건", Option.OPTION3);
        DetailQuizDto quiz3 = new DetailQuizDto("뉴스에서 언급된 소비자심리지수(CCSI)가 2021년 6월 이후 최고치를 경신한 달은 언제입니까?", "6월", "7월", "5월", Option.OPTION2);
        detailQuizService.saveQuizzes(1L, List.of(quiz1, quiz2, quiz3));

        // 뉴스 2 상세 퀴즈
        DetailQuizDto quiz4 = new DetailQuizDto("뉴스에서 소개하는 여름 제철 간식은 무엇인가요?", "수박과 복숭아", "감자와 찰옥수수", "토마토와 오이", Option.OPTION2);
        DetailQuizDto quiz5 = new DetailQuizDto("뉴스에서 언급된 장소는 어디인가요?", "서울 강남구 농협유통 하나로마트", "서울 서초구 농협유통 하나로마트 양재점", "경기도 성남시 농협유통 하나로마트", Option.OPTION2);
        DetailQuizDto quiz6 = new DetailQuizDto("뉴스에서 감자와 찰옥수수를 소개하는 사람들은 누구인가요?", "농협유통 직원들", "농민 대표들", "모델들", Option.OPTION3);
        detailQuizService.saveQuizzes(2L, List.of(quiz4, quiz5, quiz6));

        // 뉴스 3 상세 퀴즈
        DetailQuizDto quiz7 = new DetailQuizDto("경제계가 국회에서 더 강한 상법 및 노란봉투법 처리가 급물살을 타는 것에 대해 어떤 심정을 표현했습니까?", "깊은 환영과 지지를 표명했다.", "깊은 우려를 넘어 참담한 심정을 금할 수 없다.", "별다른 입장을 밝히지 않았다.", Option.OPTION2);
        DetailQuizDto quiz8 = new DetailQuizDto("경제8단체가 우려하는 상법 추가 개정의 주요 내용은 무엇입니까?", "이사의 충실의무 축소로 경영 효율성 증대", "사업재편 반대, 주요 자산 매각 등 해외 투기자본의 무리한 요구 가능성", "주주총회 의결권 강화로 기업 투명성 증대", Option.OPTION2);
        DetailQuizDto quiz9 = new DetailQuizDto("경제계는 국회의 규제 입법에 대해 어떤 점을 가장 우려하고 있으며, 국회에 무엇을 요청했습니까?", "기업 활동을 촉진하는 규제 입법을 늘리고, 노사 관계 안정을 위한 법안 처리를 요청했다.", "기업 활동을 옥죄는 규제 입법을 연이어 쏟아내는 것에 대해 기업들에게 극도의 혼란을 초래할 수 있다고 우려하며, 국익 관점에서 개정안들을 신중하게 재검토해달라고 요청했다.", "새 정부의 성장 중심 경제 정책을 지지하며, 국회는 기업 활동에 대한 규제를 완화하고 투자 활성화를 위한 법안 처리에 집중해달라고 요청했다.", Option.OPTION2);
        detailQuizService.saveQuizzes(3L, List.of(quiz7, quiz8, quiz9));

        // 뉴스 4 상세 퀴즈
        DetailQuizDto quiz10 = new DetailQuizDto("하이원 워터월드에서 열리는 'DJ 풀 파티'는 언제까지 진행되는가?", "8월 15일까지", "내달 10일까지", "8월 말까지", Option.OPTION2);
        DetailQuizDto quiz11 = new DetailQuizDto("올해 하이원리조트의 '하이원 레이저 불꽃 쇼'는 어떤 콘셉트로 진행되는가?", "바다의 신비", "우주", "동화 속 세상", Option.OPTION2);
        DetailQuizDto quiz12 = new DetailQuizDto("뉴스에서 언급된 하이원리조트의 여름철 식음료 메뉴로 올바른 것은 무엇인가?", "산상 바비큐와 오리엔 냉짬뽕", "시원한 냉면과 팥빙수", "바비큐 플래터와 해산물 파스타", Option.OPTION1);
        detailQuizService.saveQuizzes(4L, List.of(quiz10, quiz11, quiz12));

        // 뉴스 5 상세 퀴즈
        DetailQuizDto quiz13 = new DetailQuizDto("경제8단체가 상법 및 노동조합법 개정안 논의에 대해 깊은 우려를 표하며 재검토를 호소한 주된 이유는 무엇인가?", "개정안들이 기업 경영에 부담을 줄 수 있고, 한국 경제의 미래를 결정할 중요한 시점에 혼란을 초래할 수 있기 때문", "개정안들이 국회에서 통과되기까지 너무 많은 시간이 소요되어 경제 상황에 대한 대응이 늦어지고 있기 때문", "개정안들이 중소기업의 경쟁력을 약화시키고 대기업에게만 유리하게 작용하기 때문", Option.OPTION1);
        DetailQuizDto quiz14 = new DetailQuizDto("경제8단체가 언급한 '한국 경제의 미래를 결정할 중요한 분수령'은 무엇과 관련이 있는가?", "미국과의 관세 협상 마감", "새로운 성장 동력 확보를 위한 기술 개발", "국내 소비 시장의 확대", Option.OPTION1);
        DetailQuizDto quiz15 = new DetailQuizDto("경제8단체는 상법 추가 개정이 가져올 수 있는 부정적인 영향으로 무엇을 지적했는가?", "해외 투기 자본의 무리한 요구로 사업 재편 및 구조조정이 어려워질 수 있음", "노동 조합의 파업 권한이 축소되어 기업의 경영 효율성이 저하될 수 있음", "기업의 정보 공개 의무가 강화되어 영업 비밀이 침해될 수 있음", Option.OPTION1);
        detailQuizService.saveQuizzes(5L, List.of(quiz13, quiz14, quiz15));

        // 뉴스 6 상세 퀴즈
        DetailQuizDto quiz16 = new DetailQuizDto("인도 최대 IT 서비스업체 TCS가 2025/26 회계연도에 감원할 직원 수는 몇 명인가요?", "약 61만 3000명", "약 1만 2200명", "약 3847명", Option.OPTION2);
        DetailQuizDto quiz17 = new DetailQuizDto("TCS의 감원 대상이 주로 어느 직급에 해당한다고 보도되었나요?", "신입 사원 및 주니어 개발자", "최고 경영진 및 이사", "중간 관리직 및 간부 관리직", Option.OPTION3);
        DetailQuizDto quiz18 = new DetailQuizDto("인도 IT 업계가 현재 직면하고 있는 어려움으로 언급되지 않은 것은 무엇인가요?", "지속적인 인플레이션", "신규 기술 도입을 위한 투자 확대", "미국의 고관세 정책", Option.OPTION2);
        detailQuizService.saveQuizzes(6L, List.of(quiz16, quiz17, quiz18));

        // 뉴스 7 상세 퀴즈
        DetailQuizDto quiz19 = new DetailQuizDto("LG CNS가 이번에 수주한 인도네시아 시나르마스 은행의 차세대 카드 시스템 구축 사업의 총 사업 기간은 어떻게 되나요?", "3년", "6년", "10년", Option.OPTION2);
        DetailQuizDto quiz20 = new DetailQuizDto("LG CNS가 이번 사업을 통해 동남아시아 시장 공략을 본격화하는 가운데, 어떤 솔루션을 활용하여 시나르마스 은행의 카드 시스템을 구축할 예정인가요?", "AI 기반 금융 분석 솔루션", "블록체인 기반 보안 솔루션", "자체 카드 비즈니스 솔루션 '카드퍼펙트'", Option.OPTION3);
        DetailQuizDto quiz21 = new DetailQuizDto("LG CNS가 이번 인도네시아 시나르마스 은행 사업을 수주한 것은 어떤 점에서 의미가 있나요?", "국내 금융 IT 시장에서 최초로 수주한 사업이기 때문입니다.", "LG 시나르마스 합작 법인 출범 이후 금융 분야에서 수주한 첫 번째 프로젝트이자, 해외 현지 금융사의 차세대 시스템 구축 첫 사례이기 때문입니다.", "인도네시아 정부로부터 직접 발주받은 유일한 사업이기 때문입니다.", Option.OPTION2);
        detailQuizService.saveQuizzes(7L, List.of(quiz19, quiz20, quiz21));
    }

    @Transactional
    public void factQuizInit() {
        // 퀴즈가 이미 있으면 초기화 생략
        if (factQuizService.count() > 0) {
            return;
        }

        // 퀴즈 생성 로직 추가
        for(long l = 1L; l <= 7L; l++) {
            factQuizService.create(l);
        }
    }

    @Transactional
    public void dailyQuizInit() {
        // 퀴즈가 이미 있으면 초기화 생략
        if (dailyQuizService.count() > 0) {
            return;
        }

        // 퀴즈 생성 로직 추가 (예시)
        // 오늘의 뉴스 테이블의 가장 첫번째 뉴스를 가져와 해당 뉴스의 상세 퀴즈를 오늘의 퀴즈로 저장
        dailyQuizService.createDailyQuiz();
    }
}
