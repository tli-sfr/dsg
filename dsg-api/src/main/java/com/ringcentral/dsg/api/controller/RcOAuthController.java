package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.model.AdminApiModels.RcOAuthSessionResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.RcOAuthStatusResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.RcOAuthTokenRequest;
import com.ringcentral.dsg.api.rc.RcOAuthService;
import com.ringcentral.dsg.api.rc.RcSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    public RcOAuthController(RcOAuthService rcOAuthService, RcSessionService rcSessionService) {
        this.rcOAuthService = rcOAuthService;
        this.rcSessionService = rcSessionService;
    }

    @GetMapping("/status")
    public ResponseEntity<RcOAuthStatusResponse> status(@PathVariable String accountId) {
        return ResponseEntity.ok(new RcOAuthStatusResponse(
                rcOAuthService.isConfigured(),
                rcOAuthService.isConnected(accountId)));
    }

    @GetMapping("/session")
    public ResponseEntity<RcOAuthSessionResponse> session(@PathVariable String accountId) {
        return ResponseEntity.ok(rcSessionService.getSession(accountId));
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
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}
