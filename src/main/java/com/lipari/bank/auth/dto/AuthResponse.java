package com.lipari.bank.auth.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresAt,
        List<String> roles
) {}
