package com.ringcentral.dsg.api.rc;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * HttpOnly browser session cookie set after RingCentral OAuth completes in this browser.
 * {@code /rc/oauth/status} treats the account as connected only when the cookie matches the
 * requested account and a valid refresh token exists.
 */
@Component
public class RcOAuthSessionCookie {

    public static final String NAME = "dsg_rc_session";

    public ResponseCookie create(String accountId) {
        return ResponseCookie.from(NAME, accountId)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofHours(12))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
    }

    public boolean matchesAccount(String cookieValue, String accountId) {
        return accountId != null && !accountId.isBlank() && accountId.equals(cookieValue);
    }
}
