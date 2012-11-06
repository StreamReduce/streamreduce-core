package com.streamreduce.core.validation;

import com.streamreduce.util.ConnectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ConnectionTypeValidator implements ConstraintValidator<ValidConnectionType, String> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ValidConnectionType constraintAnnotation) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String connectionType, ConstraintValidatorContext constraintContext) {
        return ConnectionUtils.PROVIDER_MAP.containsKey(connectionType);
    }

}
