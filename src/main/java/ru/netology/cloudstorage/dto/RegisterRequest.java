package ru.netology.cloudstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @JsonProperty("login")
    @NotBlank
    private String login;

    @JsonProperty("password")
    @NotBlank
    private String password;
}