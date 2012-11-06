package com.streamreduce.rest.dto.response;

import java.util.List;

public class CreateComputeInventoryItemsResponseDTO {

    List<InventoryItemResponseDTO> createdItems;

    public List<InventoryItemResponseDTO> getCreatedItems() {
        return createdItems;
    }

    public void setCreatedItems(List<InventoryItemResponseDTO> createdItems) {
        this.createdItems = createdItems;
    }

}
