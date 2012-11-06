package com.streamreduce.core.service.exception;

import com.streamreduce.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -7139641724127216184L;

    public UserNotFoundException(String s) {
        super("User not Found: " + s);
    }
}
