package com.example.recruitment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 配置类
 * <p>
 * 配置JWT无状态认证体系：
 * - 禁用Session（使用Token认证）
 * - 开放登录/注册接口
 * - 其他所有接口需要携带有效JWT Token
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ========== 禁用CSRF（JWT不需要） ==========
                .csrf(AbstractHttpConfigurer::disable)

                // ========== 配置CORS ==========
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ========== 无状态会话（不使用Session）==========
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ========== 异常处理：未认证返回401 JSON而非默认页面 ==========
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"msg\":\"权限不足\",\"data\":null}");
                        })
                )

                // ========== 请求授权规则 ==========
                .authorizeHttpRequests(auth -> auth
                        // ===== 公开接口（无需Token）=====
                        // 登录/注册
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        // 模型状态接口
                        .requestMatchers("/api/model/status", "/api/model/health").permitAll()
                        // Swagger文档（开发环境）
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // CORS预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 静态资源
                        .requestMatchers(HttpMethod.GET,
                                "/favicon.ico",
                                "/*.html",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // ===== 需要认证的接口（其他全部需要JWT Token）=====
                        .anyRequest().authenticated()
                )

                // ========== 添加JWT过滤器（在用户名密码过滤器之前执行）==========
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "http://localhost:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174",
            "http://127.0.0.1:5175"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Arrays.asList("Content-Disposition", "X-Total-Count"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
