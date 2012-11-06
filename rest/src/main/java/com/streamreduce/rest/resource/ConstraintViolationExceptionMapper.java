package com.streamreduce.rest.resource;

import com.streamreduce.rest.dto.response.ConstraintViolationExceptionResponseDTO;
import com.sun.jersey.spi.resource.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return validationError(exception.getConstraintViolations());
    }

    public Response validationError(Set<ConstraintViolation<?>> violations) {
        ConstraintViolationExceptionResponseDTO dto = new ConstraintViolationExceptionResponseDTO();
        Map<String, String> violationsMap = new HashMap<String, String>();

        // Not sure if it's possible to have multiple messages per property but if so, testing should uncover
        // it and the fix is quick/simple.
        for (ConstraintViolation<?> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String violationMessage = violation.getMessage();

            // The property name is what is important, not the full path since most consumers will be RESTful
            if (propertyPath.endsWith(".") || propertyPath.length() == 0) {
                // We have a few @ScriptAssert validations which do not allow you to specify the property path.
                // Based on convention, the message will always start with the property name so we can parse it here.
                propertyPath = violationMessage.substring(0, violationMessage.indexOf(' '));
            } else if (propertyPath.indexOf('.') > -1) {
                propertyPath = propertyPath.substring(propertyPath.lastIndexOf('.'));
            }

            violationsMap.put(propertyPath, violationMessage);
        }

        dto.setViolations(violationsMap);

        return Response.status(Response.Status.BAD_REQUEST).entity(dto).build();
    }

}
