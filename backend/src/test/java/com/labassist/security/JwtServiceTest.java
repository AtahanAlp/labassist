package com.labassist.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.labassist.config.SecurityProperties;
import com.labassist.security.domain.AppUser;
import com.labassist.security.domain.UserRole;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt("test-signing-secret-that-is-at-least-32-bytes-long", 60),
                null,
                null);
        return new JwtService(properties);
    }

    private AppUser doctor() {
        AppUser user = new AppUser();
        user.setUsername("doctor");
        user.setDisplayName("Dr. Demo");
        user.setRole(UserRole.DOCTOR);
        return user;
    }

    @Test
    void generatedTokenCarriesSubjectAndRole() {
        JwtService service = jwtService();
        String token = service.generateToken(doctor());

        assertThat(token).isNotBlank();
        assertThat(service.extractUsername(token)).isEqualTo("doctor");
        assertThat(service.parse(token).getPayload().get("role", String.class)).isEqualTo("DOCTOR");
    }

    @Test
    void exposesConfiguredExpiration() {
        assertThat(jwtService().expirationMinutes()).isEqualTo(60);
    }
}
