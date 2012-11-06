package com.streamreduce.rest.dto.response;

public class InventoryItemResponseDTO extends AbstractConnectionInventoryItemResponseDTO {

    private String externalId;
    private String type;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
