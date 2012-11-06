package com.streamreduce.core.service.exception;

import com.streamreduce.core.SobaObjectNotFoundException;

public class InventoryItemNotFoundException extends SobaObjectNotFoundException {
    private static final long serialVersionUID = -9104440157245537752L;

    public InventoryItemNotFoundException(String id) {
        super("Inventory Item is not found: " + id);
    }
}
