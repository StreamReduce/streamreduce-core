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

package com.streamreduce.rest.resource.status;


import com.streamreduce.core.model.SystemInfo;
import com.streamreduce.rest.dto.response.SystemInfoResponseDTO;
import com.streamreduce.rest.resource.AbstractResource;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
@Path("status/system")
public class StatusResource extends AbstractResource {

    /**
     * Test Mule and the DB Connection
     *
     * @return - http status code and OK or an exception
     * @throws IOException - if the db connection fails
     * @response.representation.200 Returned when the status is ok from application manager
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response status() throws IOException {
        applicationManager.getSystemInfo();
        return Response
                .ok()
                .entity("OK")
                .build();
    }

    /**
     * Test if Mule is up and responding via REST
     *
     * @return - http status code and OK or an exception
     * @response.representation.200 Returned when the status is ok from application manager
     */
    @GET
    @Path("app")
    public Response appStatus() {
        return Response
                .ok()
                .entity("OK")
                .build();
    }

    /**
     * Test Mule and the DB connection
     *
     * @return - http status code, OK, and DTO or an exception
     * @throws IOException - if the db connection fails
     * @response.representation.200 Returned when the status is ok from application manager
     */
    @GET
    @Path("verbose")
    public Response verboseStatus() throws IOException {
        return Response
                .ok()
                .entity(toDTO(applicationManager.getSystemInfo()))
                .build();
    }

    public SystemInfoResponseDTO toDTO(SystemInfo systemStatus) {
        SystemInfoResponseDTO dto = new SystemInfoResponseDTO();
        dto.setCreated(systemStatus.getCreated());
        dto.setModified(systemStatus.getModified());
        dto.setId(systemStatus.getId());
        dto.setAppVersion(systemStatus.getAppVersion());
        dto.setBuildNumber(systemStatus.getBuildNumber());
        dto.setDbVersion(systemStatus.getDbVersion());
        dto.setPatchLevel(systemStatus.getPatchLevel());
        return dto;
    }


}
