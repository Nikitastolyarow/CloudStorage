package ru.netology.cloudstorage.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.netology.cloudstorage.entity.User;
import ru.netology.cloudstorage.service.AuthService;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;

    @Autowired
    public JwtAuthenticationFilter(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = request.getHeader("auth-token");
        System.out.println("[JWT FILTER] Необработанный заголовок токена: " + token);

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            System.out.println("[JWT FILTER] Токен после удаления владельца: " + token);
        }

        if (token != null && !token.isEmpty()) {
            Optional<User> userOptional = authService.getUserByToken(token);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("[JWT FILTER] Аутентифицированный пользователь: " + user.getLogin());
            }
        }

        filterChain.doFilter(request, response);
    }
}
