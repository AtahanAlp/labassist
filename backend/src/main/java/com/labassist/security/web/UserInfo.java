package com.labassist.security.web;

import com.labassist.security.domain.AppUser;

/** Non-sensitive user details returned to the client. */
public record UserInfo(
        String username,
        String displayName,
        String role) {

    public static UserInfo from(AppUser user) {
        return new UserInfo(user.getUsername(), user.getDisplayName(), user.getRole().name());
    }
}
