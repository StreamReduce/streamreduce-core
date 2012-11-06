package com.streamreduce.core.service.exception;

import com.streamreduce.DuplicateItemException;

public class UsernameUnavailableException extends DuplicateItemException {

    private static final long serialVersionUID = 1L;

    public UsernameUnavailableException(String s) {
        super("Username is not available " + s);
    }

}
