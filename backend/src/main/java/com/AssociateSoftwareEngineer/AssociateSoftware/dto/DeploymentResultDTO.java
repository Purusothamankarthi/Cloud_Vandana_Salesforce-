package com.AssociateSoftwareEngineer.AssociateSoftware.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds Metadata API deployment result information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentResultDTO {

    private String deploymentId;
    private String status;
    private boolean success;
    private String errorMessage;
    private int componentsDeployed;
    private int componentsFailed;
}
