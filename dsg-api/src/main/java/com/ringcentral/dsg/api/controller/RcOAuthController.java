package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.model.AdminApiModels.RcOAuthSessionResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.RcOAuthStatusResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.RcOAuthTokenRequest;
import com.ringcentral.dsg.api.rc.RcOAuthService;
import com.ringcentral.dsg.api.rc.RcOAuthSessionCookie;
import com.ringcentral.dsg.api.rc.RcSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dsg/v1/{accountId}/rc/oauth")
public class RcOAuthController {

    private final RcOAuthService rcOAuthService;
    private final RcSessionService rcSessionService;
    private final RcOAuthSessionCookie sessionCookie;

    public RcOAuthController(
            RcOAuthService rcOAuthService,
            RcSessionService rcSessionService,
            RcOAuthSessionCookie sessionCookie) {
        this.rcOAuthService = rcOAuthService;
        this.rcSessionService = rcSessionService;
        this.sessionCookie = sessionCookie;
    }

    @GetMapping("/status")
    public ResponseEntity<RcOAuthStatusResponse> status(
            @PathVariable String accountId,
            @CookieValue(value = RcOAuthSessionCookie.NAME, required = false) String browserSessionAccountId) {
        boolean connected = rcOAuthService.hasValidRefreshToken(accountId)
                && sessionCookie.matchesAccount(browserSessionAccountId, accountId);
        return ResponseEntity.ok(new RcOAuthStatusResponse(rcOAuthService.isConfigured(), connected));
    }

    @GetMapping("/session")
    public ResponseEntity<RcOAuthSessionResponse> session(
            @PathVariable String accountId,
            @CookieValue(value = RcOAuthSessionCookie.NAME, required = false) String browserSessionAccountId) {
        RcOAuthSessionResponse session = rcSessionService.getSession(accountId);
        if (!sessionCookie.matchesAccount(browserSessionAccountId, session.rcAccountId())) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, sessionCookie.create(session.rcAccountId()).toString())
                    .body(session);
        }
        return ResponseEntity.ok(session);
    }

    @GetMapping("/authorize-url")
    public ResponseEntity<Map<String, String>> authorizeUrl(@PathVariable String accountId) {
        return ResponseEntity.ok(rcOAuthService.buildAuthorizeUrl(accountId));
    }

    @PostMapping("/token")
    public ResponseEntity<?> exchangeToken(
            @PathVariable String accountId,
            @Valid @RequestBody RcOAuthTokenRequest request) {
        rcOAuthService.exchangeAuthorizationCode(accountId, request.code(), request.state());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.create(accountId).toString())
                .body(Map.of("status", "OK"));
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> logout(@PathVariable String accountId) {
        rcOAuthService.logout(accountId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.clear().toString())
                .build();
    }
}
