package com.lipari.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh token obbligatorio") String refreshToken
) {}
