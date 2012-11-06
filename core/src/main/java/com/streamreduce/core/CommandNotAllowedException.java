package com.streamreduce.core;

public class CommandNotAllowedException extends Exception {
    private static final long serialVersionUID = -3779889324983548747L;

    public CommandNotAllowedException(String msg) {
        super(msg);
    }

}
