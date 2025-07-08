package ru.netology.cloudstorage.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.netology.cloudstorage.entity.Token;
import ru.netology.cloudstorage.entity.User;
import ru.netology.cloudstorage.repository.TokenRepository;
import ru.netology.cloudstorage.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final String jwtSecret;
    private final long jwtExpiration;

    public AuthService(
            UserRepository userRepository,
            TokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration}") long jwtExpiration
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
    }

    public String authenticate(String login, String password) {
        Optional<User> userOptional = userRepository.findByLogin(login);
        if (userOptional.isEmpty()) {
            System.out.println("Пользователь не найден");
            return null;
        }
        User user = userOptional.get();

        System.out.println("Попытка входа пользователя в систему: " + login);
        System.out.println("Пароль от запроса: " + password);
        System.out.println("Сохраненный хэш: " + user.getPasswordHash());

        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
        System.out.println("Пароль совпадает? " + matches);

        if (!matches) {
            System.out.println("Неверные учетные данные");
            return null;
        }

        // Деактивируем все существующие токены
        tokenRepository.findAll().stream()
                .filter(token -> token.getUser().equals(user) && token.isActive())
                .forEach(token -> {
                    token.setActive(false);
                    tokenRepository.save(token);
                });

        // Генерация JWT токена
        String token = Jwts.builder()
                .setSubject(user.getLogin())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();

        // Сохранение токена в базу
        Token tokenEntity = new Token();
        tokenEntity.setUser(user);
        tokenEntity.setAuthToken(token);
        tokenEntity.setActive(true);
        tokenEntity.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(tokenEntity);

        return token;
    }
    public void logout(String authToken) {
        Optional<Token> tokenOptional = tokenRepository.findByAuthToken(authToken);
        if (tokenOptional.isPresent()) {
            Token token = tokenOptional.get();
            token.setActive(false);
            tokenRepository.save(token);
        }
    }

    public Optional<User> getUserByToken(String authToken) {
        Optional<Token> tokenOptional = tokenRepository.findByAuthToken(authToken);
        if (tokenOptional.isPresent() && tokenOptional.get().isActive()) {
            return Optional.of(tokenOptional.get().getUser());
        }
        return Optional.empty();
    }

    public boolean register(String login, String password) {
        if (userRepository.findByLogin(login).isPresent()) {
            return false; // пользователь уже есть
        }

        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode(password)); // шифруем пароль
        userRepository.save(user);
        return true;
    }
}