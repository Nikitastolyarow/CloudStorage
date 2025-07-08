package ru.netology.cloudstorage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {

    @NotBlank
    private String login;

    @NotBlank
    private String password;

    public LoginRequest() {
    }

}