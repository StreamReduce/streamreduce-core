package com.streamreduce.rest.resource.swagger;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.jaxrs.listing.ApiListing;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/api-docs")
@Api("/api-docs")
@Produces({"application/json"})
public class ApiListingResourceJSON extends ApiListing {}
