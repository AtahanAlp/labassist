package com.labassist.security.domain;

import com.labassist.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** An application user (doctor or admin) with a BCrypt-hashed password. */
@Getter
@Setter
@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;
}
