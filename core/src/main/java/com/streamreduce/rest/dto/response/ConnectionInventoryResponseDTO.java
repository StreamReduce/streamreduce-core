package com.streamreduce.rest.dto.response;

import java.util.List;

public class ConnectionInventoryResponseDTO {
    
    private List<InventoryItemResponseDTO> inventoryItems;

    public List<InventoryItemResponseDTO> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<InventoryItemResponseDTO> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

}
