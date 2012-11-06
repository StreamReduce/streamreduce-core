package com.streamreduce.rest.dto.response;

import java.util.Map;

public class ConstraintViolationExceptionResponseDTO {

    Map<String, String> violations;

    public Map<String, String> getViolations() {
        return violations;
    }

    public void setViolations(Map<String, String> violations) {
        this.violations = violations;
    }

}
