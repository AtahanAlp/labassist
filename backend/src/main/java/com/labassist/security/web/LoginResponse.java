package com.labassist.security.web;

/** Successful login result: the bearer token and the authenticated user. */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        UserInfo user) {

    public static LoginResponse of(String token, long expiresInMinutes, UserInfo user) {
        return new LoginResponse(token, "Bearer", expiresInMinutes, user);
    }
}
