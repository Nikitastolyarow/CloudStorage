package ru.netology.cloudstorage.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.netology.cloudstorage.dto.ErrorResponse;
import ru.netology.cloudstorage.dto.LoginRequest;
import ru.netology.cloudstorage.dto.LoginResponse;
import ru.netology.cloudstorage.service.AuthService;

@RestController
@RequestMapping("/cloud")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        System.out.println("Попытка регистрации: " + request.getLogin() + ", " + request.getPassword());
        boolean created = authService.register(request.getLogin(), request.getPassword());
        if (!created) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Пользователь уже существует", 400));
        }
        return ResponseEntity.ok().build();
    }

@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    String requestId = java.util.UUID.randomUUID().toString();
    System.out.println("Попытка входа в систему [" + requestId + "]: " + request.getLogin() + ", " + request.getPassword());
    String token = authService.authenticate(request.getLogin(), request.getPassword());
    if (token == null) {
        System.out.println("Неверные учетные данные [" + requestId + "]");
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Неверные учетные данные", 400));
    }
    return ResponseEntity.ok(new LoginResponse(token));
}

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String authToken) {
        System.out.println("Попытка выхода из системы с помощью токена: " + authToken);
        authService.logout(authToken);
        return ResponseEntity.ok().build();
    }
}