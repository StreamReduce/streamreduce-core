package com.streamreduce;

public abstract class NotFoundException extends NodeableException {

    private static final long serialVersionUID = 4196548817949018735L;

    public NotFoundException(String id) {
        super("Could not find object " + id);
    }

}
