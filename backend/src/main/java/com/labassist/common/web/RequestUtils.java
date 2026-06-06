package com.labassist.common.web;

import jakarta.servlet.http.HttpServletRequest;

/** Small HTTP request helpers. */
public final class RequestUtils {

    private RequestUtils() {
    }

    /** Client IP, honouring a single X-Forwarded-For hop (e.g. behind nginx). */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
