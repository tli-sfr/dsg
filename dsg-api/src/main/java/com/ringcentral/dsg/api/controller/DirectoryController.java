package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryConfigRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryUpdateRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.ErrorResponse;
import com.ringcentral.dsg.api.service.AdminApiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dsg/v1/{accountId}")
public class DirectoryController {

    private final AdminApiService adminApiService;

    public DirectoryController(AdminApiService adminApiService) {
        this.adminApiService = adminApiService;
    }

    @PostMapping("/directory")
    public ResponseEntity<Void> createDirectory(@PathVariable String accountId, @Valid @RequestBody DirectoryConfigRequest request) {
        adminApiService.createDirectory(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/directory")
    public ResponseEntity<Void> updateDirectory(@PathVariable String accountId, @RequestBody DirectoryUpdateRequest request) {
        adminApiService.updateDirectory(accountId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/directory")
    public ResponseEntity<DirectoryResponse> getDirectory(@PathVariable String accountId) {
        return ResponseEntity.ok(adminApiService.getDirectory(accountId));
    }

    @PutMapping("/directory/oauth")
    public ResponseEntity<Void> putDirectoryOAuth(@PathVariable String accountId, @Valid @RequestBody DirectoryOAuthRequest request) {
        adminApiService.putOAuth(accountId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/directory/oauth")
    public ResponseEntity<DirectoryOAuthResponse> getDirectoryOAuth(@PathVariable String accountId) {
        return ResponseEntity.ok(adminApiService.getOAuth(accountId));
    }

    @PostMapping("/directory/oauth/test")
    public ResponseEntity<?> testDirectoryOAuth(@PathVariable String accountId) {
        if (adminApiService.testOAuth(accountId)) {
            return ResponseEntity.ok(Map.of("status", "OK"));
        }
        return ResponseEntity.badRequest().body(new ErrorResponse("DIRECTORY_AUTH_TEST_FAILED", "OAuth config not found"));
    }
}
