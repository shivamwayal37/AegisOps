package io.aegisops.agent.remediation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemediationResult {
    private boolean success;
    private String action;
    private String message;
    private String errorMessage;
    private Instant timestamp;
}
