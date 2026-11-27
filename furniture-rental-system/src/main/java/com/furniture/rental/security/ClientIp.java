package com.furniture.rental.security;

import jakarta.servlet.http.HttpServletRequest;

/** Works out the address a request really came from. */
final class ClientIp {

    private ClientIp() {
    }

    /**
     * With trustForwardedFor, the last X-Forwarded-For entry is used. That is the one added by
     * our own proxy, so a client cannot fake it. Without it, the direct connection address is used.
     */
    static String of(HttpServletRequest request, boolean trustForwardedFor) {
        if (trustForwardedFor) {
            String header = request.getHeader("X-Forwarded-For");
            if (header != null && !header.isBlank()) {
                String[] entries = header.split(",");
                String last = entries[entries.length - 1].trim();
                if (!last.isEmpty()) {
                    return last;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
