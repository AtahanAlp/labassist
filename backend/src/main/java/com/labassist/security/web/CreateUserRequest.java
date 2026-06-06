package com.labassist.security.web;

import com.labassist.security.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload for an admin creating a new account. */
public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
        String displayName,
        @NotNull UserRole role) {
}
