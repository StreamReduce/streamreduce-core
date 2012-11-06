package com.streamreduce;

public class DuplicateItemException extends NodeableException {

    private static final long serialVersionUID = 8186695244890109310L;

    public DuplicateItemException() {
    }

    public DuplicateItemException(String s) {
        super(s);
    }

    public DuplicateItemException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DuplicateItemException(Throwable throwable) {
        super(throwable);
    }
}
