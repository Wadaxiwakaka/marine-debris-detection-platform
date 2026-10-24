package com.yolo.garbage.config;

import com.yolo.garbage.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        logger.info("Request URI: {}", requestURI);
        
        // 跳过登录请求的JWT验证
        if (requestURI.endsWith("/user/login")) {
            logger.info("Skipping JWT validation for login request");
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // 从请求头中获取token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            try {
                // 解析token，获取用户名
                Claims claims = jwtUtil.parseToken(token);
                username = claims.get("username", String.class);
                logger.info("Token parsed successfully for user: {}", username);
            } catch (ExpiredJwtException e) {
                logger.warn("Token expired: {}", e.getMessage());
            } catch (MalformedJwtException e) {
                logger.warn("Invalid token: {}", e.getMessage());
            } catch (UnsupportedJwtException e) {
                logger.warn("Unsupported token: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                logger.warn("Token claims is empty: {}", e.getMessage());
            }
        } else {
            logger.warn("No Authorization header found");
        }

        // 如果token有效，且当前没有认证信息，则设置认证
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (!jwtUtil.isTokenExpired(token)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        username, null, null);
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                logger.info("Authentication set for user: {}", username);
            } else {
                logger.warn("Token is expired");
            }
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}
