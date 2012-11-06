package com.streamreduce.rest.dto.response;

import java.util.List;

public class AuthTypeResponsesDTO {
    List<AuthTypeResponseDTO> authTypes;

    public List<AuthTypeResponseDTO> getAuthTypes() {
        return authTypes;
    }

    public void setAuthTypes(List<AuthTypeResponseDTO> authTypes) {
        this.authTypes = authTypes;
    }
}
