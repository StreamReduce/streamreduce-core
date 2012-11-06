package com.streamreduce.core.service.exception;

import com.streamreduce.NodeableException;

public class InvalidStateException extends NodeableException {
    private static final long serialVersionUID = -859961730134297869L;

    public InvalidStateException(String s) {
        super("Object is in an invalid state: " + s);
    }
}
