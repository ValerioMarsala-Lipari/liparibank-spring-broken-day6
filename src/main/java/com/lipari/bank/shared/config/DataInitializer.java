package com.lipari.bank.shared.config;

import com.lipari.bank.auth.BankUserRepository;
import com.lipari.bank.auth.RoleRepository;
import com.lipari.bank.auth.model.BankUser;
import com.lipari.bank.auth.model.Role;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer {

    private final BankUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        if (userRepository.count() > 0) {
            return;
        }

        Role adminRole = roleRepository.save(new Role("ROLE_ADMIN"));
        Role customerRole = roleRepository.save(new Role("ROLE_CUSTOMER"));

        BankUser admin = new BankUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEnabled(true);
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        BankUser customer = new BankUser();
        customer.setUsername("customer");
        customer.setPassword(passwordEncoder.encode("customer123"));
        customer.setEnabled(true);
        customer.setRoles(Set.of(customerRole));
        userRepository.save(customer);

        log.info("Utenti di test inizializzati: admin, customer");
    }
}
