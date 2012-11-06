package com.streamreduce.rest.resource.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.streamreduce.Constants;
import com.streamreduce.analytics.MetricCriteria;
import com.streamreduce.analytics.MetricName;
import com.streamreduce.core.dao.MetricDAO;
import com.streamreduce.rest.resource.AbstractResource;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * REST endpoints for getting access to metrics for reporting and external analysis.
 */
@Component
@Path("metrics")
public class MetricResource extends AbstractResource {

    @Autowired
    private MetricDAO metricDAO;

    private static final Set<String> REQUIRED_PARAMS = ImmutableSet.of(
            "ACCOUNT_ID",
            "METRIC_NAME",
            "GRANULARITY"
    );
    private static final Set<String> NUMERICAL_PARAMS = ImmutableSet.of(
            "END_TIME",
            "PAGE_NUM",
            "PAGE_SIZE",
            "START_TIME"
    );

    @GET
    public Response getMetrics(@Context UriInfo context) {
        MultivaluedMap<String, String> queryParams = context.getQueryParameters();
        Map<String, List<String>> criteria = new HashMap<String, List<String>>();
        String accountId;
        String metricName;
        String granularity;
        Long startTime = null;
        Long endTime = null;
        int pageNum = 1; // Reasonable default
        int pageSize = 1; // Reasonable default

        // TODO: Check that the user is logged in and accessing metrics that are either global or in their account

        // Ensure required query parameters are present, in some form
        for (String queryParam : REQUIRED_PARAMS) {
            if (!queryParams.containsKey(queryParam) || !StringUtils.hasText(queryParams.getFirst(queryParam))) {
                return error(queryParam + " is a required, non-empty query parameter.",
                             Response.status(Response.Status.BAD_REQUEST));
            }
        }

        accountId = queryParams.getFirst("ACCOUNT_ID");
        metricName = queryParams.getFirst("METRIC_NAME");
        granularity = queryParams.getFirst("GRANULARITY");

        // Further validate the METRIC_NAME to ensure it's a valid metric name
        try {
            MetricName.valueOf(metricName);
        } catch (IllegalArgumentException e) {
            return error(metricName + " is not a valid METRIC_NAME.", Response.status(Response.Status.BAD_REQUEST));
        }

        // Further validate the GRANULARITY by checking the value is one of the canned values
        if (!Constants.KNOWN_GRANULARITY_NAMES.keySet().contains(granularity)) {
            return error(granularity + " is not a valid GRANULARITY.", Response.status(Response.Status.BAD_REQUEST));
        }

        // Gather/Validate numerical parameters
        for (String queryParam : NUMERICAL_PARAMS) {
            if (queryParams.containsKey(queryParam)) {
                try {
                    Long.parseLong(queryParams.getFirst(queryParam));
                } catch (NumberFormatException e) {
                    return error(queryParam + " parameter should be a number.",
                                 Response.status(Response.Status.BAD_REQUEST));
                }
            }
        }

        if (queryParams.containsKey("START_TIME")) {
            startTime = Long.parseLong(queryParams.getFirst("START_TIME"));
        }

        if (queryParams.containsKey("END_TIME")) {
            endTime = Long.parseLong(queryParams.getFirst("END_TIME"));
        }

        if (queryParams.containsKey("PAGE_NUM")) {
            pageNum = Integer.parseInt(queryParams.getFirst("PAGE_NUM"));
        }

        if (queryParams.containsKey("PAGE_SIZE")) {
            pageSize = Integer.parseInt(queryParams.getFirst("PAGE_SIZE"));
        }

        // Gather/Validate criteria
        for (String queryParam : queryParams.keySet()) {
            if (queryParam.startsWith("criteria.")) {
                String[] paramParts = queryParam.split("\\.");

                if (paramParts.length != 2) {
                    return error("Your metric criteria is improperly structured, expected: " +
                                         "criteria.YOUR_CRITERIA_NAME=YOUR_CRITERIA_VALUE.",
                                 Response.status(Response.Status.BAD_REQUEST));
                }

                try {
                    MetricCriteria.valueOf(paramParts[1]);
                } catch (IllegalArgumentException e) {
                    return error(paramParts[1] + " is not a valid METRIC_CRITERIA.",
                                 Response.status(Response.Status.BAD_REQUEST));
                }

                criteria.put(paramParts[1], queryParams.get(queryParam));
            }
        }

        return Response.ok(
                JSONObject.fromObject(ImmutableMap.of(
                        "metrics", metricDAO.getMetrics(accountId, metricName, criteria,
                                                        Constants.KNOWN_GRANULARITY_NAMES.get(granularity),
                                                        startTime, endTime, pageNum, pageSize).toString())
                )
        ).build();
    }

}
