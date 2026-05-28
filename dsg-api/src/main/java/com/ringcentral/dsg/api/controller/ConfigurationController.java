package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DeprovisioningRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DeprovisioningResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleListResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.SchedulerRequest;
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

@RestController
@RequestMapping("/dsg/v1/{accountId}")
public class ConfigurationController {

    private final AdminApiService adminApiService;

    public ConfigurationController(AdminApiService adminApiService) {
        this.adminApiService = adminApiService;
    }

    @PostMapping("/scheduler")
    public ResponseEntity<Void> configureScheduler(@PathVariable String accountId, @Valid @RequestBody SchedulerRequest request) {
        adminApiService.configureScheduler(accountId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/attribute-mapping")
    public ResponseEntity<Void> saveAttributeMapping(@PathVariable String accountId, @Valid @RequestBody AttributeMappingRequest request) {
        adminApiService.saveAttributeMapping(accountId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rules")
    public ResponseEntity<ProvisioningRuleListResponse> listRules(@PathVariable String accountId) {
        return ResponseEntity.ok(adminApiService.listRules(accountId));
    }

    @PostMapping("/rule")
    public ResponseEntity<Void> createProvisioningRule(@PathVariable String accountId, @Valid @RequestBody ProvisioningRuleRequest request) {
        adminApiService.saveRule(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/deprovisioning")
    public ResponseEntity<DeprovisioningResponse> getDeprovisioning(@PathVariable String accountId) {
        return ResponseEntity.ok(adminApiService.getDeprovisioning(accountId));
    }

    @PutMapping("/deprovisioning")
    public ResponseEntity<Void> saveDeprovisioning(
            @PathVariable String accountId, @Valid @RequestBody DeprovisioningRequest request) {
        adminApiService.saveDeprovisioning(accountId, request);
        return ResponseEntity.ok().build();
    }
}
