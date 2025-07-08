package ru.netology.cloudstorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudstorage.entity.Token;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByAuthToken(String authToken);
}