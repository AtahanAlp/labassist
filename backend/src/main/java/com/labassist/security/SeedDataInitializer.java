package com.labassist.security;

import com.labassist.config.SecurityProperties;
import com.labassist.security.domain.AppUser;
import com.labassist.security.domain.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the configured bootstrap accounts on first startup if they don't exist. */
@Slf4j
@Component
public class SeedDataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    public SeedDataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                               SecurityProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        SecurityProperties.Seed seed = properties.seed();
        seedUser(seed.doctorUsername(), seed.doctorPassword(), seed.doctorDisplayName(), UserRole.DOCTOR);
        seedUser(seed.adminUsername(), seed.adminPassword(), seed.adminDisplayName(), UserRole.ADMIN);
    }

    private void seedUser(String username, String rawPassword, String displayName, UserRole role) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return;
        }
        if (userRepository.existsByUsername(username)) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Seeded {} account '{}'", role, username);
    }
}
