package com.streamreduce.core.service.exception;

import com.streamreduce.NotFoundException;

public class InventoryItemAlertNotFoundException extends NotFoundException {
    private static final long serialVersionUID = -1264004617156262086L;

    public InventoryItemAlertNotFoundException(String id) {
        super("Inventory Item Alert Not Found " + id);
    }
}
