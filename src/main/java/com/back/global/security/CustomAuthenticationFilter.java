package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.global.standard.util.Ut;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final MemberService memberService;
    private final Rq rq;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        logger.debug("Processing request for " + request.getRequestURI());

        try { // 필터 체인에서 다음 필터로 요청을 전달
            work(request, response, filterChain);
        } catch (ServiceException e) {
            RsData<Void> rsData = e.getRsData();
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(rsData.code());
            String jsonResponse = Ut.json.toString(rsData);
            if (jsonResponse == null) {
                jsonResponse = "{\"resultCode\":\"" + rsData.code() + "\",\"msg\":\"" + rsData.message() + "\"}";
            }
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            throw e;
        }
    }

    private void work(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();


        // SecurityConfig에서 permitAll()로 설정된 경로들은 필터를 통과시키기
        if (
                uri.startsWith("/swagger-ui/") ||
                        uri.startsWith("/v3/api-docs/") ||
                        uri.startsWith("/swagger-resources/") ||
                        uri.startsWith("/h2-console") ||
                        (method.equals("GET") && uri.equals("/api/news")) ||
                        (method.equals("GET") && uri.equals("/api/members/rank")) ||
                        (method.equals("GET") && uri.startsWith("/api/news/")) ||
                        (method.equals("GET") && uri.equals("/api/quiz/fact")) ||
                        (method.equals("GET") && uri.equals("/api/quiz/fact/category")) ||
                        (method.equals("POST") && uri.equals("/api/members/login")) ||
                        (method.equals("POST") && uri.equals("/api/members/join"))
        ) {

            filterChain.doFilter(request, response);
            return;
        }

        // 인증 필수 URL (authenticated, hasRole)
        boolean requiresAuth =
                uri.startsWith("/api/quiz/detail/") ||
                        uri.startsWith("/api/quiz/daily/") ||
                        uri.startsWith("/api/admin/") ||
                        uri.equals("/api/members/info") ||
                        uri.equals("/api/members/logout") ||
                        uri.equals("/api/members/withdraw") ||
                        (method.equals("GET") && uri.matches("/api/quiz/fact/\\d+")) ||
                        (method.equals("POST") && uri.matches("/api/quiz/fact/submit/\\d+"));

        // 인증이 필요하지 않은 URL이면 그냥 통과
        if (!requiresAuth) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey;
        String accessToken;

        String headerAuthorization = rq.getHeader("Authorization", "");

        if (!headerAuthorization.isBlank()) {
            if (!headerAuthorization.startsWith("Bearer "))
                throw new ServiceException(401, "Authorization 헤더가 Bearer 형식이 아닙니다.");

            String[] headerAuthorizationBits = headerAuthorization.split(" ", 3);

            apiKey = headerAuthorizationBits[1];
            accessToken = headerAuthorizationBits.length == 3 ? headerAuthorizationBits[2] : "";
        } else {
            apiKey = rq.getCookieValue("apiKey", "");
            accessToken = rq.getCookieValue("accessToken", "");
        }

        logger.debug("apiKey : " + apiKey);
        logger.debug("accessToken : " + accessToken);

        boolean isApiKeyExists = !apiKey.isBlank();
        boolean isAccessTokenExists = !accessToken.isBlank();

        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response);
            return;
        }

        Member member = null;
        boolean isAccessTokenValid = false;

        // accessToken이 있으면 우선 검증
        if (isAccessTokenExists) {
            Map<String, Object> payload = memberService.payload(accessToken);

            if (payload != null) {
                long id = (long) payload.get("id");
                String email = (String) payload.get("email");
                String name = (String) payload.get("name");
                String role = (String) payload.get("role");
                member = new Member(id, email, name, role);
                isAccessTokenValid = true;
            }
        }

        // accessToken이 없으면, apiKey로 member 조회
        if (member == null) {
            member = memberService
                    .findByApiKey(apiKey)
                    .orElseThrow(() -> new ServiceException(401, "API 키가 유효하지 않습니다."));
        }

        // accessToken이 만료됐으면 새로 발급
        if (isAccessTokenExists && !isAccessTokenValid) {
            String actorAccessToken = memberService.genAccessToken(member);

            rq.setCrossDomainCookie("accessToken", actorAccessToken, 60 * 20);
            rq.setHeader("Authorization", actorAccessToken);
        }

        // SecurityContext에 인증 정보 저장
        UserDetails user = new SecurityUser(
                member.getId(),
                member.getEmail(),
                member.getName(),
                "",
                member.getAuthorities()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                user.getPassword(),
                user.getAuthorities()
        );
        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);

    }
}