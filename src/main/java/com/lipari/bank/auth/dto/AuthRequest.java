package com.lipari.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Username obbligatorio") String username,
        @NotBlank(message = "Password obbligatoria") String password
) {}
