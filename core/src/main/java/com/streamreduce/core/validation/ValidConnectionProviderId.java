package com.streamreduce.core.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target( { METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ConnectionProviderIdValidator.class)
@Documented
public @interface ValidConnectionProviderId {

    String message() default "{connection.providerId.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
