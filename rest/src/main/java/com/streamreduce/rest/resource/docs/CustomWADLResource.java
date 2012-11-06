package com.streamreduce.rest.resource.docs;

import com.sun.jersey.spi.resource.Singleton;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Singleton
@Component("docs/customWADLResource")
@Path("/")
public class CustomWADLResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomWADLResource.class);

    private String indexDocument;
    private Map<String, String> wadlDocuments = new HashMap<String, String>();
    private Map<String, String> htmlDocuments = new HashMap<String, String>();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public synchronized Response getDefault() {
        try {
            if (indexDocument == null) {
                indexDocument = readFileFromClasspathURL(getClass().getResource("/index.html"));
            }
        } catch (Exception e) {
            LOGGER.error("Could not marshal wadl application", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Could not marshal wadl application: " + e.getMessage()).build();
        }

        return Response.ok(indexDocument).build();
    }

    @GET
    @Path("{module}/wadl")
    @Produces(MediaType.APPLICATION_XML)
    public synchronized Response getWadl(@PathParam("module") String module) {
        String wadlDocument = wadlDocuments.get(module);

        try {
            if (wadlDocument == null) {
                wadlDocument = readFileFromClasspathURL(getClass().getResource("/application-" + module + ".wadl"));

                wadlDocuments.put("module", wadlDocument);
            }
        } catch (Exception e) {
            LOGGER.error("Could not marshal wadl application", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Could not marshal wadl application: " + e.getMessage()).build();
        }

        return Response.ok(wadlDocument).build();
    }

    @GET
    @Path("{module}/html")
    @Produces(MediaType.TEXT_HTML)
    public synchronized Response getHtml(@PathParam("module") String module) {
        String htmlDocument = htmlDocuments.get(module);

        try {
            if (htmlDocument == null) {
                htmlDocument = readFileFromClasspathURL(getClass().getResource("/application-" + module + ".html"));

                htmlDocuments.put(module, htmlDocument);
            }
        } catch (Exception e) {
            LOGGER.error("Could not marshal wadl application", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Could not marshal wadl application: " + e.getMessage()).build();
        }

        return Response.ok(htmlDocument).build();
    }

    private String readFileFromClasspathURL(URL url) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            while ((line = in.readLine()) != null)
                sb.append(line);
        } finally {
            in.close();
        }

        return sb.toString();
    }

}
