package com.streamreduce.core.service.exception;

public class InvalidCredentialsException extends Exception {

    private static final long serialVersionUID = -5690155104176372438L;

    public InvalidCredentialsException(String msg) {
        super(msg);
    }

    public InvalidCredentialsException(Throwable cause) {
        super("The credentials associated with this connection are invalid: " + cause.getMessage(), cause);
    }

}
