package com.back.global.security

import com.back.global.rsData.RsData
import com.back.global.standard.util.Ut
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter,
    private val customOAuth2LoginSuccessHandler: CustomOAuth2LoginSuccessHandler,
    private val customOAuth2AuthorizationRequestResolver: CustomOAuth2AuthorizationRequestResolver
) {

    private val permitAllGetPaths = listOf(
        "/api/news",
        "/api/news/*/**",
        "/api/quiz/fact",
        "/api/quiz/fact/category",
        "/api/members/rank"
    )

    private val permitAllPostPaths = listOf(
        "/api/members/login",
        "/api/members/join"
    )

    private val authenticatedPaths = listOf(
        "/api/quiz/detail/*/**",
        "/api/histories",
        "/api/quiz/daily/*/**",
        "/api/members/info",
        "/api/quiz/fact/{id}",
        "/api/quiz/fact/submit/{id}",
        "/api/members/withdraw",
        "/api/members/logout"
    )

    private val adminPaths = listOf(
        "/api/admin/*/**"
    )

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                // permitAll
                authorize("/favicon.ico", permitAll)
                authorize("/h2-console/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-resources/**", permitAll)

                permitAllGetPaths.forEach { authorize(HttpMethod.GET, it, permitAll) }
                permitAllPostPaths.forEach { authorize(HttpMethod.POST, it, permitAll) }

                // authenticated
                authenticatedPaths.forEach { authorize(it, authenticated) }

                // admin
                adminPaths.forEach { authorize(it, hasRole("ADMIN")) }

                // 그 외 인증 필요
                authorize("/api/*/**", authenticated)

                // 나머지
                authorize(anyRequest, permitAll)
            }

            headers { frameOptions { sameOrigin = true } }
            csrf { disable() }
            formLogin { disable() }
            logout { disable() }
            httpBasic { disable() }

            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }

            oauth2Login {
                authenticationSuccessHandler = customOAuth2LoginSuccessHandler
                authorizationEndpoint { authorizationRequestResolver = customOAuth2AuthorizationRequestResolver }
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(customAuthenticationFilter)

            exceptionHandling {
                authenticationEntryPoint = AuthenticationEntryPoint { _, response, _ ->
                    response.contentType = "$APPLICATION_JSON_VALUE;charset=UTF-8"
                    response.status = 401
                    response.writer.write(
                        Ut.json.toString(RsData<Void>(401, "로그인 후 이용해주세요.", null))
                    )
                }

                accessDeniedHandler = AccessDeniedHandler { _, response, _ ->
                    response.contentType = "$APPLICATION_JSON_VALUE;charset=UTF-8"
                    response.status = 403
                    response.writer.write(
                        Ut.json.toString(RsData<Void>(403, "권한이 없습니다.", null))
                    )
                }
            }
        }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000", "https://news-ox.vercel.app")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE")
            allowCredentials = true
            allowedHeaders = listOf("*")
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuration)
            registerCorsConfiguration("/admin/**", configuration)
        }
    }
}
