package com.AssociateSoftwareEngineer.AssociateSoftware.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Salesforce Account validation rule.
 * Populated from Tooling API query results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRuleDTO {

    private String id;
    private String fullName;
    private String name;
    private String description;
    private String errorMessage;
    private String errorDisplayField;
    private String formula;
    private boolean active;
}
