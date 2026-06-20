package com.example.demo.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "authUserId";
    public static final String ATTR_CUSTOMER_ID = "authCustomerId";
    public static final String ATTR_ROLE = "authRole";

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Browser preflight never sends Authorization — must pass through for CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/products")) {
            return true;
        }
        return path.startsWith("/api/hello")
                || path.startsWith("/api/wallet-check");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"reason\":\"Thiếu token đăng nhập\"}");
            return;
        }

        try {
            Claims claims = jwtService.parseToken(header.substring(7));
            request.setAttribute(ATTR_USER_ID, jwtService.getUserId(claims));
            request.setAttribute(ATTR_CUSTOMER_ID, jwtService.getCustomerId(claims));
            request.setAttribute(ATTR_ROLE, jwtService.getRole(claims));
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"reason\":\"Token không hợp lệ hoặc đã hết hạn\"}");

            return;
        }
    }
}
