package com.streamreduce.core;

import com.streamreduce.NotFoundException;

public class SobaObjectNotFoundException extends NotFoundException {
    private static final long serialVersionUID = 9060468962471773255L;

    public SobaObjectNotFoundException(String id) {
        super("Soba Object is not found: " + id);
    }
}
