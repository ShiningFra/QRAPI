package com.example.QRAPI.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

	final String authorizationHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();

        // Exclure les routes publiques
        if (requestURI.contains("/reserve")) {
System.out.println("Ok 1\n");
            filterChain.doFilter(request, response);
            return;
        }

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
System.out.println("Ok 2 \n");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization header is missing or malformed");
            return;
        }

        String token = extractToken(request);

        if (jwtUtil.validateToken(token)) {
System.out.println("Ok 3\n");
            String fournisseur = jwtUtil.extractFournisseur(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(fournisseur, null, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
System.out.println("Ok 4\n");
        filterChain.doFilter(request, response);
System.out.println("Ok 5\n");
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
