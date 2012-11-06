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

package com.streamreduce.rest.resource.api;

import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.connections.OAuthEnabledConnectionProvider;
import com.streamreduce.core.service.OAuthTokenCacheService;
import com.streamreduce.rest.resource.AbstractResource;
import com.streamreduce.util.JSONObjectBuilder;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.NoSuchElementException;

@Component("oauthResource")
@Path("api/oauth")
public class OAuthResource extends AbstractResource {

    @Autowired
    ConnectionProviderFactory connectionProviderFactory;

    @Autowired
    OAuthTokenCacheService oAuthTokenCacheService;

    /**
     * Returns all details for a given provider in auth.  Presently this includes only an authorizationUrl that
     * users should be redirected to on the provider website in order to start an Oauth handshake.
     *
     * @response.representation.200.doc OAuth provider details
     * @response.representation.201.mediaType application/json
     * @response.representation.400.doc If the provideId does not exist
     * @response.representation.400.mediaType text/plain
     * @response.representation.500.doc If a general exception occurs.
     * @response.representation.500.mediaType text/plain
     *
     * @param providerId - A valid provider id.
     * @return Json Payload with k/v pairs for each detail about an OAuth enabled provider.
     */
    @Path("providers/{providerId}")
    @GET
    public Response getOAuthDetailsForProvider(@PathParam("providerId") String providerId) {
        try {
            OAuthEnabledConnectionProvider connectionProvider =
                    connectionProviderFactory.oauthEnabledConnectionProviderFromId(providerId);
            String authorizationUrl = connectionProvider.getAuthorizationUrl();
            JSONObject providerOAuthDetail = new JSONObjectBuilder()
                    .add("authorizationUrl",authorizationUrl)
                    .build();
            return Response.ok(providerOAuthDetail).build();
        } catch (NoSuchElementException e) {
            return error(providerId + " is not a registered oauth provider in Nodeable",
                    Response.status(Response.Status.BAD_REQUEST));
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return error(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
