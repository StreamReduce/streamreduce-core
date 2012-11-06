package com.streamreduce;


public class InvalidUserAliasException extends RuntimeException {
    @SuppressWarnings("unused")
    public InvalidUserAliasException() {
    }

    @SuppressWarnings("unused")
    public InvalidUserAliasException(String s) {
        super(s);
    }

    @SuppressWarnings("unused")
    public InvalidUserAliasException(String s, Throwable throwable) {
        super(s, throwable);
    }

    @SuppressWarnings("unused")
    public InvalidUserAliasException(Throwable throwable) {
        super(throwable);
    }
}
