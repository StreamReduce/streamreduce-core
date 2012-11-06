package com.streamreduce.core.service.exception;

import com.streamreduce.NotFoundException;

public class AccountNotFoundException extends NotFoundException {
    private static final long serialVersionUID = -8369109511456695809L;

    public AccountNotFoundException(String id) {
        super("Account not found: " + id);
    }
}
