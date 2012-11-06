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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.service.SearchService;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.core.service.UserService;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import com.streamreduce.rest.resource.AbstractResource;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Path("api/search/messages")
public class SearchMessageResource extends AbstractResource {

    @Autowired
    UserService userService;
    @Autowired
    SecurityService securityService;
    @Autowired
    SearchService searchService;

    /**
     *
     * @response.representation.200.doc Search results
     * @response.representation.200.mediaType application/json
     * @response.representation.500.doc If a general exception occurs.
     * @response.representation.404.mediaType text/plain
     *
     * @param query
     * @param resourceName
     * @param uriInfo
     * @return the response body
     */
    @POST
    @Path("{resourceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchMessagesPassthrough(JSONObject query,
                                              @PathParam("resourceName") String resourceName,
                                              @Context UriInfo uriInfo) {
        try {
            Account currentAccount = securityService.getCurrentUser().getAccount();
            ImmutableMap<String, String> queryParameters = ImmutableMap.copyOf(flattenValues(uriInfo.getQueryParameters()));
            List<SobaMessage> sobaMessages = searchService.searchMessages(currentAccount, resourceName, queryParameters, query);

            boolean fullText = queryParameters.get("fullText") != null ? Boolean.valueOf(queryParameters.get("fullText")) : false;

            return Response
                    .ok(SobaMessageResponseDTO.fromSobaMessages(sobaMessages,fullText))
                    .build();
        } catch (Exception e) {
            String errorMessage = "Unable to perform search: " + e.getMessage();
            logger.info(errorMessage,e);
            return error(errorMessage, Response.serverError());
        }
    }

    /**
     * Compresses the values in a Map&lt;String,Collection&lt;String&gt;&gt; to Strings by selecting the first valid
     * value in the Collection.  This method is presumed to only be called with the value of UriInfo.getQueryParameters.
     *
     * @param map
     * @return
     */
    private Map<String, String> flattenValues(MultivaluedMap<String, String> map) {
        HashMap<String, String> newMap = new HashMap<String, String>();
        for (String key : map.keySet()) {
            Collection<String> value = map.get(key);
            Optional<String> possibleFind = Iterables.tryFind(value, new Predicate<String>() {
                @Override
                public boolean apply(@Nullable String input) {
                    return (input != null && StringUtils.isNotBlank(input));
                }
            });
            if (possibleFind.isPresent()) {
                newMap.put(key, possibleFind.get());
            }
        }
        return newMap;
    }

}
