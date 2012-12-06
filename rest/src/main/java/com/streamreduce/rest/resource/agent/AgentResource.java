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

package com.streamreduce.rest.resource.agent;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.rest.resource.AbstractResource;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
@Path("agent/metrics")
public class AgentResource extends AbstractResource {

    /**
     * Takes a JSON payload representing an agent metrics payload, validates it and
     * persists validated payloads.  This will also result in a stream message
     * with the agent metric information in the message stream.  (Note: The JSON
     * payload can be either a singular agent metrics payload or an array of agent
     * metrics payload objects.)
     * <br /><br />
     * The request object should be structured as described below:
     *
     * <ul class="indented">
     *     <li><b>node_id*:</b> The instance id as known by the cloud provider (Must correspond to a cloud inventory item's node id)</li>
     *     <li><b>data*:</b> The JSON object describing the agent metric payload
     *         <ul>
     *             <li><b>generated*:</b> The timestamp of when the metric was gathered/created (Format: yyyy-MM-dd'T'HH:mm:ss.SSSSSS)</li>
     *             <li>... (This data can be whatever you want really and since we do not validate this, no need in documenting it at this time.)</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * <b>* denotes a required field.</b>
     *
     * @param jsonString the raw JSON payload representing either a singular agent
     * metrics payload or an array of singular agent metrics payloads
     *
     * @return the result of the request
     *
     * @response.representation.405.doc Returned if the request was invalid in any way:
     * Invalid agent metrics payload structure, invalid cloud inventory item id in the
     * payload or whenever any deeper level exception occurs
     *
     * @response.representation.201.doc Returned if the request was successful
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAgentMetricsEntry(String jsonString) {

        if (isEmpty(jsonString)) {
            return error("Missing payload", Response.status(Response.Status.BAD_REQUEST));
        }

        try {
            JSONObject json = JSONObject.fromObject(jsonString);

            if (!json.has("node_id")) {
                return error("Request is missing the node id to associate the metrics with.",
                        Response.status(Response.Status.BAD_REQUEST));
            }

            if (!json.has("data")) {
                return error("Request is missing the 'data'.", Response.status(Response.Status.BAD_REQUEST));
            }

            try {
                json.getJSONArray("data");
            } catch (JSONException je) {
                return error("'data' should be an array.", Response.status(Response.Status.BAD_REQUEST));
            }

            String nodeId = json.getString("node_id");
            List<InventoryItem> inventoryItems =
                    applicationManager.getInventoryService().getInventoryItemsForExternalId(nodeId);

            if (inventoryItems == null || inventoryItems.isEmpty()) {
                return error("'node_id' does not correspond with an inventory item, no metrics stored.",
                        Response.status(Response.Status.BAD_REQUEST));
            }

            // we might have multiple inventoryItems with this nodeId
            // if keys are shared across the account boundary (you never know!)
            for (InventoryItem inventoryItem : inventoryItems) {
                JSONArray metrics = json.getJSONArray("data");

                for (Object rawMetric : metrics) {
                    JSONObject metric = (JSONObject)rawMetric;
                    String generated = metric.getString("generated");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
                    Date dateGenerated;

                    try {
                        dateGenerated = df.parse(generated);
                    } catch (ParseException pe) {
                        logger.error("Unable to parse the generated date of the metric.", pe);
                        dateGenerated = new Date();
                    }

                    Map<String, Object> eventContext = new HashMap<>();

                    eventContext.put("activityGenerated", dateGenerated);
                    eventContext.put("activityPayload", metric);

                    inventoryItem.addHashtag("agent");

                    // Create the event stream entry
                    Event event = applicationManager.getEventService().createEvent(EventId.ACTIVITY,
                                                                                   inventoryItem,
                                                                                   eventContext);

                    // create the message
                    // note: all messages will have the #agent tag now.
                    applicationManager.getMessageService().sendAccountMessage(event,
                                                                              inventoryItem,
                                                                              inventoryItem.getConnection(),
                                                                              dateGenerated.getTime(),
                                                                              MessageType.AGENT,
                                                                              inventoryItem.getHashtags(),
                                                                              null);

                }

            }

            return Response
                    .status(Response.Status.CREATED)
                    .build();

        } catch (Exception e) {
            logger.error("Agent Exception", e);
            return error(e.getMessage(), Response.status(Response.Status.BAD_REQUEST));
        }
    }

}
