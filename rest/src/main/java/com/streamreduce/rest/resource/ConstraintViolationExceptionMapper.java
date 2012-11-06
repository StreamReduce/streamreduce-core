/*
 * Copyright 2012 Nodeable Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
