package com.lipari.bank.auth;

import com.lipari.bank.auth.dto.AuthRequest;
import com.lipari.bank.auth.dto.AuthResponse;
import com.lipari.bank.auth.dto.RegisterRequest;
import com.lipari.bank.auth.model.BankUser;
import com.lipari.bank.auth.model.Role;
import com.lipari.bank.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final BankUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username già in uso: " + request.username());
        }

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Ruolo ROLE_CUSTOMER non trovato"));

        BankUser newUser = new BankUser();
        newUser.setUsername(request.username());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setEnabled(true);
        newUser.setRoles(Set.of(customerRole));

        userRepository.save(newUser);
        log.info("Nuovo utente registrato: {}", request.username());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        return buildAuthResponse(userDetails);
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        log.info("Login effettuato: {}", request.username());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        return buildAuthResponse(userDetails);
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new IllegalArgumentException("Refresh token non valido");
        }

        String tokenType = jwtService.extractTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new IllegalArgumentException("Il token fornito non è un refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new IllegalArgumentException("Refresh token scaduto o invalido");
        }

        log.info("Access token rinnovato per: {}", username);

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                jwtService.extractExpiration(newAccessToken).getTime(),
                userDetails.getAuthorities().stream().map(Object::toString).toList()
        );
    }

    private AuthResponse buildAuthResponse(UserDetails userDetails) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.extractExpiration(accessToken).getTime(),
                userDetails.getAuthorities().stream().map(Object::toString).toList()
        );
    }
}
