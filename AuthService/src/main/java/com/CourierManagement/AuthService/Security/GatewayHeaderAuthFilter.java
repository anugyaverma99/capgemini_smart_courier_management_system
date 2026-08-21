package com.CourierManagement.AuthService.Security;

import com.CourierManagement.AuthService.Security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public GatewayHeaderAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {


        String email = request.getHeader("X-User-Email");
        String role  = request.getHeader("X-User-Role");
        

        if (email != null && role != null) {
            String springRole = role.startsWith("ROLE_")
                    ? role
                    : "ROLE_" + role;
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority(springRole))
                    );
                SecurityContextHolder.getContext().setAuthentication(auth);
            
        }

        filterChain.doFilter(request, response);
    }
}