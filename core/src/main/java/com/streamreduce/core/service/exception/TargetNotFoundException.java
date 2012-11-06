package com.streamreduce.core.service.exception;

import com.streamreduce.NotFoundException;

public class TargetNotFoundException extends NotFoundException {

    private static final long serialVersionUID = 7749763077065502101L;

    public TargetNotFoundException(String s) {
        super(s);
    }

}
