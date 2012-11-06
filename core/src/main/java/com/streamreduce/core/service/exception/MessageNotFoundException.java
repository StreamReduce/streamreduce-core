package com.streamreduce.core.service.exception;

import com.streamreduce.core.SobaObjectNotFoundException;

public class MessageNotFoundException extends SobaObjectNotFoundException {

    private static final long serialVersionUID = -283050350881825266L;

    public MessageNotFoundException(String id) {
        super(id);
    }
}
