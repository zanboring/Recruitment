package com.example.recruitment.config;

import com.example.recruitment.util.JwtUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.GrantedAuthority;

/**
 * JWT 认证过滤器
 * <p>
 * 拦截所有HTTP请求，从请求头中提取JWT Token，
 * 验证通过后将用户身份信息设置到 SecurityContext 中。
 * </p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Token 在请求头中的名称
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        if (request == null || response == null || filterChain == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && jwtUtil != null && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                List<GrantedAuthority> authorities;
                if (role != null && ("ADMIN".equalsIgnoreCase(role) || "admin".equals(role))) {
                    authorities = AuthorityUtils.createAuthorityList(
                        "ROLE_ADMIN", "ROLE_USER",
                        "job:write", "job:delete",
                        "crawl:manage", "data:cleanup"
                    );
                } else {
                    authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.warn("JWT认证失败: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token
     *
     * @param request HTTP请求
     * @return 纯净的Token字符串（去掉"Bearer "前缀），无Token返回null
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
