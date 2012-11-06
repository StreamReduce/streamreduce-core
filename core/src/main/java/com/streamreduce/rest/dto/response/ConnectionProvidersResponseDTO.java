package com.streamreduce.rest.dto.response;

import java.util.List;

public class ConnectionProvidersResponseDTO {

    List<ConnectionProviderResponseDTO> providers;

    public List<ConnectionProviderResponseDTO> getProviders() {
        return providers;
    }

    public void setProviders(List<ConnectionProviderResponseDTO> providers) {
        this.providers = providers;
    }

}
