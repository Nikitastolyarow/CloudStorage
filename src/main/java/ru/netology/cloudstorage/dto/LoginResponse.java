package ru.netology.cloudstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LoginResponse {
    @JsonProperty("auth-token")
    private final String authToken;
}
