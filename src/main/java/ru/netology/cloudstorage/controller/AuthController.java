package ru.netology.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.netology.cloudstorage.dto.LoginErrorResponse;
import ru.netology.cloudstorage.dto.LoginRequest;
import ru.netology.cloudstorage.dto.LoginResponse;
import ru.netology.cloudstorage.dto.RegisterRequest;
import ru.netology.cloudstorage.config.JwtTokenService;
import ru.netology.cloudstorage.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authManager;
    private final JwtTokenService jwtTokenService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        logger.debug("Login attempt for user: {}", request.getLogin());
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtTokenService.generateToken(userDetails);

            logger.info("Login successful for user: {}", request.getLogin());
            return ResponseEntity.ok(new LoginResponse("Bearer " + token));
        } catch (AuthenticationException e) {
            logger.warn("Login failed for user: {}. Error: {}", request.getLogin(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    new LoginErrorResponse(
                            new String[]{"Неверные учетные данные"},
                            new String[]{"Неверные учетные данные"}
                    )
            );
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        logger.debug("Register attempt for user: {}", request.getLogin());
        userService.registerUser(request);
        logger.info("User registered: {}", request.getLogin());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/logout")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        logger.debug("Logout attempt for token: {}", token);
        if (token != null && token.startsWith("Bearer ")) {
            jwtTokenService.invalidateToken(token.substring(7));
        }
        logger.info("Logout successful for token: {}", token);
        Map<String, String> redirect = new HashMap<>();
        redirect.put("redirect", "/login");
        return ResponseEntity.ok().body(redirect);
    }
}