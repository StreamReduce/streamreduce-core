package com.streamreduce.core.service.exception;

import com.streamreduce.NodeableException;

public class ObjectNotSubribableException extends NodeableException {

    public ObjectNotSubribableException(String s) {
        super("Can not follow " + s);
    }

    public ObjectNotSubribableException() {
    }
}
