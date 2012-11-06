package com.streamreduce.core.service.exception;

import com.streamreduce.NotFoundException;

public class FeedNotFoundException extends NotFoundException {

    private static final long serialVersionUID = 91869141970942485L;

    public FeedNotFoundException(String id) {
        super(id);
    }

}
