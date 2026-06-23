package com.AssociateSoftwareEngineer.AssociateSoftware.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User profile information retrieved from Salesforce.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private String userId;
    private String name;
    private String email;
    private String orgId;
    private String instanceUrl;
    private String username;
}
