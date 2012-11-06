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

package com.streamreduce.rest.resource.authentication;


import com.streamreduce.Constants;
import com.streamreduce.ValidationException;
import com.streamreduce.core.model.APIAuthenticationToken;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.rest.resource.AbstractResource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
@Path("authentication/login")
public class AuthenticationResource extends AbstractResource {

    /**
     * Used to generate a Nodeable authentication token. All public API requests are made using the token generated here.
     * <p/>
     * The initial request for a token arrives via BASIC authentication and is handled by the Apache Shiro filter (clear text username and password).
     * <p/>
     * At this point we know who they are and they they are auth'd, we just have to create the custom token and return it to them.
     *
     * @return a valid http status code, and the X-Auth-Token in the header on success
     * @resource.representation.204 if the operation was a success
     * @resource.representation.404 returned if the user is not found
     * @resource.representation.500 returned if invalid params are provided
     */
    @POST
    public Response login() {

        APIAuthenticationToken apiToken;
        try {
            apiToken = applicationManager.getSecurityService().issueAuthenticationToken(applicationManager.getSecurityService().getCurrentUser());
        } catch (UserNotFoundException e) {
            return error(e.getMessage(), Response.status(Response.Status.NOT_FOUND));
        } catch (ValidationException e) {
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }

        // return the token as a custom header value
        return Response.ok()
                .header(Constants.NODEABLE_AUTH_TOKEN, apiToken.getToken())
                .status(Response.Status.NO_CONTENT)
                .build();
    }


}
