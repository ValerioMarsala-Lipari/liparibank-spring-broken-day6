package com.lipari.bank.auth;

import com.lipari.bank.auth.model.BankUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class LipariBankUserDetailsService implements UserDetailsService {

    private final BankUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Caricamento utente: {}", username);

        BankUser bankUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Utente non trovato: {}", username);
                    return new UsernameNotFoundException("Utente non trovato: " + username);
                });

        return User.builder()
                .username(bankUser.getUsername())
                .password(bankUser.getPassword())
                .authorities(Collections.emptyList())
                .disabled(!bankUser.isEnabled())
                .build();
    }
}
