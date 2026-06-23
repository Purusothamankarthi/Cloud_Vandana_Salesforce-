package com.AssociateSoftwareEngineer.AssociateSoftware.service;

import com.AssociateSoftwareEngineer.AssociateSoftware.dto.DeploymentResultDTO;
import com.AssociateSoftwareEngineer.AssociateSoftware.dto.ValidationRuleDTO;
import com.AssociateSoftwareEngineer.AssociateSoftware.exception.SalesforceException;
import com.AssociateSoftwareEngineer.AssociateSoftware.util.MetadataDeployUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for managing Salesforce Account validation rules.
 *
 * Responsibilities:
 * - Fetch all validation rules from the Account object via Tooling API
 * - Toggle individual or bulk validation rules
 * - Deploy changes via Metadata API
 * - Check deployment status
 */
@Slf4j
@Service
public class ValidationRuleService {

    private final SalesforceApiService salesforceApiService;

    public ValidationRuleService(SalesforceApiService salesforceApiService) {
        this.salesforceApiService = salesforceApiService;
    }

    /**
     * Get all validation rules on the Account object.
     * Uses Tooling API SOQL query.
     */
    @SuppressWarnings("unchecked")
    public List<ValidationRuleDTO> getAllValidationRules(String accessToken, String instanceUrl) {
        log.info("Fetching all Account validation rules");

        // Step 1: Query for basic fields to get the IDs (cannot query Metadata/FullName for multiple records)
        String query = "SELECT Id, ValidationName, Active, Description, ErrorMessage, "
                + "ErrorDisplayField, EntityDefinition.QualifiedApiName "
                + "FROM ValidationRule "
                + "WHERE EntityDefinition.QualifiedApiName = 'Account'";

        Map<String, Object> response = salesforceApiService.toolingQuery(query, accessToken, instanceUrl);

        List<Map<String, Object>> records = (List<Map<String, Object>>) response.get("records");

        if (records == null || records.isEmpty()) {
            log.info("No validation rules found on Account object");
            return Collections.emptyList();
        }

        List<ValidationRuleDTO> rules = new ArrayList<>();
        
        // Step 2: Fetch the full details (including Metadata and FullName) for each rule individually
        for (Map<String, Object> record : records) {
            String ruleId = (String) record.get("Id");
            try {
                Map<String, Object> fullRecord = salesforceApiService.toolingGet("ValidationRule", ruleId, accessToken, instanceUrl);
                ValidationRuleDTO dto = mapRecordToDTO(fullRecord);
                rules.add(dto);
            } catch (Exception e) {
                log.error("Failed to fetch full details for ValidationRule {}: {}", ruleId, e.getMessage());
            }
        }

        log.info("Retrieved {} validation rules", rules.size());
        return rules;
    }

    /**
     * Toggle a single validation rule (active ↔ inactive).
     * Fetches current state, flips the active flag, and deploys via Metadata API.
     */
    public DeploymentResultDTO toggleValidationRule(String ruleName, String accessToken, String instanceUrl) {
        log.info("Toggling validation rule: {}", ruleName);

        // First, get current rule state
        ValidationRuleDTO currentRule = findRuleByName(ruleName, accessToken, instanceUrl);
        if (currentRule == null) {
            throw new SalesforceException("Validation rule not found: " + ruleName);
        }

        boolean newActiveState = !currentRule.isActive();
        log.info("Changing rule {} from {} to {}", ruleName, currentRule.isActive(), newActiveState);

        try {
            // Build and deploy metadata package
            byte[] zipBytes = MetadataDeployUtil.buildSingleRuleDeployZip(
                    "Account", ruleName, newActiveState,
                    currentRule.getDescription(),
                    currentRule.getErrorMessage(),
                    currentRule.getFormula(),
                    currentRule.getErrorDisplayField()
            );

            String deploymentId = salesforceApiService.deployMetadata(zipBytes, accessToken, instanceUrl);

            return DeploymentResultDTO.builder()
                    .deploymentId(deploymentId)
                    .status("InProgress")
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error toggling validation rule: {}", ruleName, e);
            throw new SalesforceException("Failed to toggle rule " + ruleName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Toggle multiple validation rules at once.
     * Builds a single deployment package for all rules.
     */
    public DeploymentResultDTO toggleMultipleValidationRules(List<String> ruleNames,
                                                             String accessToken, String instanceUrl) {
        log.info("Toggling {} validation rules", ruleNames.size());

        // Get all current rules
        List<ValidationRuleDTO> allRules = getAllValidationRules(accessToken, instanceUrl);

        // Build toggle maps
        List<Map<String, Object>> ruleMaps = new ArrayList<>();
        for (String ruleName : ruleNames) {
            ValidationRuleDTO rule = allRules.stream()
                    .filter(r -> r.getName().equals(ruleName) || r.getFullName().equals(ruleName)
                            || r.getFullName().endsWith("." + ruleName))
                    .findFirst()
                    .orElseThrow(() -> new SalesforceException("Rule not found: " + ruleName));

            Map<String, Object> ruleMap = new HashMap<>();
            ruleMap.put("fullName", "Account." + (rule.getName() != null ? rule.getName() : ruleName));
            ruleMap.put("active", !rule.isActive()); // Toggle
            ruleMap.put("description", rule.getDescription());
            ruleMap.put("errorMessage", rule.getErrorMessage());
            ruleMap.put("formula", rule.getFormula());
            ruleMap.put("errorDisplayField", rule.getErrorDisplayField());
            ruleMaps.add(ruleMap);
        }

        try {
            byte[] zipBytes = MetadataDeployUtil.buildDeploymentZip(ruleMaps);
            String deploymentId = salesforceApiService.deployMetadata(zipBytes, accessToken, instanceUrl);

            return DeploymentResultDTO.builder()
                    .deploymentId(deploymentId)
                    .status("InProgress")
                    .success(true)
                    .componentsDeployed(ruleNames.size())
                    .build();

        } catch (Exception e) {
            log.error("Error toggling multiple rules", e);
            throw new SalesforceException("Failed to toggle rules: " + e.getMessage(), e);
        }
    }

    /**
     * Check deployment status.
     */
    public DeploymentResultDTO checkDeploymentStatus(String deploymentId,
                                                      String accessToken, String instanceUrl) {
        Map<String, String> status = salesforceApiService.checkDeployStatus(deploymentId, accessToken, instanceUrl);

        return DeploymentResultDTO.builder()
                .deploymentId(status.get("id"))
                .status(status.get("status"))
                .success("true".equalsIgnoreCase(status.get("success")))
                .errorMessage(status.get("errorMessage"))
                .build();
    }

    /**
     * Find a specific rule by name.
     */
    private ValidationRuleDTO findRuleByName(String ruleName, String accessToken, String instanceUrl) {
        List<ValidationRuleDTO> rules = getAllValidationRules(accessToken, instanceUrl);
        return rules.stream()
                .filter(r -> r.getName().equals(ruleName) || r.getFullName().equals(ruleName)
                        || r.getFullName().endsWith("." + ruleName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Map a Tooling API record to ValidationRuleDTO.
     */
    @SuppressWarnings("unchecked")
    private ValidationRuleDTO mapRecordToDTO(Map<String, Object> record) {
        // The Metadata field contains nested rule details
        Map<String, Object> metadata = (Map<String, Object>) record.get("Metadata");

        String fullName = (String) record.get("FullName");
        String validationName = (String) record.get("ValidationName");

        // Extract name from FullName (format: "Account.RuleName")
        String name = fullName != null && fullName.contains(".")
                ? fullName.substring(fullName.lastIndexOf('.') + 1)
                : validationName;

        boolean active = false;
        String description = "";
        String errorMessage = "";
        String errorDisplayField = "";
        String formula = "";

        if (metadata != null) {
            active = Boolean.TRUE.equals(metadata.get("active"));
            description = (String) metadata.getOrDefault("description", "");
            errorMessage = (String) metadata.getOrDefault("errorMessage", "");
            errorDisplayField = (String) metadata.getOrDefault("errorDisplayField", "");
            formula = (String) metadata.getOrDefault("errorConditionFormula", "");
        } else {
            // Fallback to direct fields
            active = Boolean.TRUE.equals(record.get("Active"));
            description = (String) record.getOrDefault("Description", "");
            errorMessage = (String) record.getOrDefault("ErrorMessage", "");
            errorDisplayField = (String) record.getOrDefault("ErrorDisplayField", "");
            formula = (String) record.getOrDefault("ErrorConditionFormula", "");
        }

        return ValidationRuleDTO.builder()
                .id((String) record.get("Id"))
                .fullName(fullName)
                .name(name)
                .description(description)
                .errorMessage(errorMessage)
                .errorDisplayField(errorDisplayField)
                .formula(formula)
                .active(active)
                .build();
    }
}
