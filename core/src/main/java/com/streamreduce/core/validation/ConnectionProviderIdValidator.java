package com.streamreduce.core.validation;

import com.streamreduce.connections.ConnectionProvider;
import com.streamreduce.util.ConnectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ConnectionProviderIdValidator implements ConstraintValidator<ValidConnectionProviderId, String> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ValidConnectionProviderId constraintAnnotation) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String providerId, ConstraintValidatorContext constraintContext) {
        for (ConnectionProvider provider : ConnectionUtils.getAllProviders()) {
            if (provider.getId().equals(providerId)) {
                return true;
            }
        }

        return false;
    }

}
