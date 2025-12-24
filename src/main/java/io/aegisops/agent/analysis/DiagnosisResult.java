package io.aegisops.agent.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResult {
    private String rootCause;
    private Double confidence;
    private String recommendedAction;
    private String reasoning;
    private boolean safe;
    private String source; // "RULE_ENGINE" or "LLM"
}
