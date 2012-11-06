package com.streamreduce;

public class ValidationException extends NodeableException {

    private static final long serialVersionUID = 6111115739087100813L;

    public ValidationException(String s) {
        super("Validation Error:" + s);
    }

}
