package com.streamreduce;

public class NodeableException extends Exception {

    private static final long serialVersionUID = 5977946528932197670L;

    public NodeableException() {
    }

    public NodeableException(String s) {
        super(s);
    }

    public NodeableException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NodeableException(Throwable throwable) {
        super(throwable);
    }
}
