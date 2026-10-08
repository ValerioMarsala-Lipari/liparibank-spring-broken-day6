package com.lipari.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username obbligatorio")
        @Size(min = 3, max = 50, message = "Username deve essere tra 3 e 50 caratteri")
        String username,

        @NotBlank(message = "Password obbligatoria")
        @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
        String password
) {}
