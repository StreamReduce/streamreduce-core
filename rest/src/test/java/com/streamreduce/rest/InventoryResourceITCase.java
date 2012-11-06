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

package com.streamreduce.rest;

import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.rest.dto.response.ConnectionInventoryResponseDTO;
import com.streamreduce.rest.dto.response.ConnectionResponseDTO;
import com.streamreduce.rest.dto.response.InventoryItemResponseDTO;
import net.sf.json.JSONObject;
import org.codehaus.jackson.map.type.TypeFactory;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.compute.predicates.NodePredicates;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationScope;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryResourceITCase extends AbstractInContainerTestCase {

    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private InventoryService inventoryService;

    private ConnectionResponseDTO cloud;
    private String authnToken;
    private String awsAccessKey;
    private String awsSecretKey;
    private boolean skipInventoryCleanup = false;

    public InventoryResourceITCase() {
        super();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        authnToken = login(testUsername, testUsername);
        awsAccessKey = cloudProperties.getString("nodeable.aws.accessKeyId");
        awsSecretKey = cloudProperties.getString("nodeable.aws.secretKey");

        JSONObject json = new JSONObject();
        JSONObject credentialsObject = new JSONObject();

        credentialsObject.put("identity", awsAccessKey);
        credentialsObject.put("credential", awsSecretKey);

        json.put("alias", "Inventory Item Name Test CloudConnection");
        json.put("credentials", credentialsObject);
        json.put("description", "This cloud is for testing inventory item names.");
        json.put("providerId", ProviderIdConstants.AWS_PROVIDER_ID);
        json.put("type", ConnectionTypeConstants.CLOUD_TYPE);

        cloud = jsonToObject(makeRequest(connectionsBaseUrl, "POST", json, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionResponseDTO.class));
    }

    @Override
    public void tearDown() throws Exception {
        // Clean up the created clouds
        if (cloud != null) {
            connectionService.deleteConnection(connectionService.getConnection(cloud.getId()));
        }

        // Destroy all nodes if not done already
        try {
            if (!skipInventoryCleanup) {
                getComputeService(cloud).destroyNodesMatching(NodePredicates.runningInGroup(testJcloudsInstanceGroup));
            }
        } catch (RuntimeException re) {
            // This can happen if the AWS communication is a slow but does not mean failure, especially a test failure
        }

        super.tearDown();
    }

    @Test
    @Ignore
    public void testInventoryName() throws Exception {
        NodeMetadata newNode = createDummyAWSEC2Node(cloud, testJcloudsInstanceGroup);

        refreshCloudInventoryItemCache(connectionService.getConnection(cloud.getId()), authnToken);

        String cloudInventoryUrl = connectionsBaseUrl + "/" + cloud.getId() + "/inventory";
        ConnectionInventoryResponseDTO cloudInventory =
                jsonToObject(makeRequest(cloudInventoryUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionInventoryResponseDTO.class));

        InventoryItemResponseDTO originalDTO = null;

        for (InventoryItemResponseDTO inventoryItem : cloudInventory.getInventoryItems()) {
            if (inventoryItem.getExternalId().equals(newNode.getProviderId())) {
                originalDTO = inventoryItem;
                // Default name for nodes without name is the node id
                assertEquals(inventoryItem.getExternalId(), inventoryItem.getAlias());
                break;
            }
        }

        assertNotNull(originalDTO);

        // Change the name of the inventory item
        JSONObject json = new JSONObject();

        json.put("alias", "Inventory Item Name");

        InventoryItemResponseDTO responseDTO =
                jsonToObject(makeRequest(inventoryItemBaseUrl + "/" + originalDTO.getId(), "PUT", json, authnToken),
                TypeFactory.defaultInstance().constructType(InventoryItemResponseDTO.class));

        assertEquals(json.getString("alias"), responseDTO.getAlias());
    }

    @Test
    @Ignore
    public void testInventoryItemRebootAndDestroy() throws Exception {
        NodeMetadata newNode = createDummyAWSEC2Node(cloud, testJcloudsInstanceGroup);

        refreshCloudInventoryItemCache(connectionService.getConnection(cloud.getId()), authnToken);

        String cloudInventoryUrl = connectionsBaseUrl + "/" + cloud.getId() + "/inventory";
        ConnectionInventoryResponseDTO cloudInventory =
                jsonToObject(makeRequest(cloudInventoryUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionInventoryResponseDTO.class));

        InventoryItemResponseDTO iiDTO = null;

        for (InventoryItemResponseDTO inventoryItem : cloudInventory.getInventoryItems()) {
            if (inventoryItem.getExternalId().equals(newNode.getProviderId())) {
                iiDTO = inventoryItem;
                break;
            }
        }

        assertNotNull(iiDTO);

        String currentState =  iiDTO.getPayload().getString("state");

        assertEquals(NodeState.RUNNING.toString(), currentState);

        assertEquals("200",
                makeRequest(inventoryItemBaseUrl + "/" +  iiDTO.getId() + "/reboot", "PUT", null, authnToken));

        cloudInventory = jsonToObject(makeRequest(cloudInventoryUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(InventoryItemResponseDTO.class));


        for (InventoryItemResponseDTO inventoryItem : cloudInventory.getInventoryItems()) {
            if (inventoryItem.getExternalId().equals(newNode.getProviderId())) {
                iiDTO = inventoryItem;
                break;
            }
        }

        // There is no good way to test a reboot since it seems that these node reboots are too fast to see a state
        // change so just ensure they are running after reboot.
        currentState =  iiDTO.getPayload().getString("state");

        assertEquals(NodeState.RUNNING.toString(), currentState);

        // Destroy the node
        makeRequest(inventoryItemBaseUrl + "/" +  iiDTO.getId(), "DELETE", null, authnToken);

        cloudInventory = jsonToObject(makeRequest(cloudInventoryUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionInventoryResponseDTO.class));


        for (InventoryItemResponseDTO inventoryItem : cloudInventory.getInventoryItems()) {
            if (inventoryItem.getExternalId().equals(newNode.getProviderId())) {
                assertEquals(NodeState.TERMINATED.toString(), inventoryItem.getPayload().getString("state"));
                break;
            }
        }

        skipInventoryCleanup = true;
    }

    @Test
    @Ignore
    public void testInventoryItemRegionAndZone() throws Exception {
        NodeMetadata newNode = createDummyAWSEC2Node(cloud, testJcloudsInstanceGroup);

        refreshCloudInventoryItemCache(connectionService.getConnection(cloud.getId()), authnToken);

        String cloudInventoryUrl = connectionsBaseUrl + "/" + cloud.getId() + "/inventory";
        ConnectionInventoryResponseDTO cloudInventory =
                jsonToObject(makeRequest(cloudInventoryUrl, "GET", null, authnToken),
                TypeFactory.defaultInstance().constructType(ConnectionInventoryResponseDTO.class));

        InventoryItemResponseDTO inventoryItemDTO = null;

        for (InventoryItemResponseDTO inventoryItem : cloudInventory.getInventoryItems()) {
            if (inventoryItem.getExternalId().equals(newNode.getProviderId())) {
                inventoryItemDTO = inventoryItem;
                break;
            }
        }

        assertNotNull(inventoryItemDTO);

        // Make sure the zone and region is accurate
        Location location = newNode.getLocation();
        boolean zoneMatches = false;
        boolean regionMatches = false;

        while (location != null) {
            JSONObject payload = JSONObject.fromObject(inventoryItemDTO.getPayload().toString());
            String region = inventoryService.getLocationByScope(payload, LocationScope.REGION).getId();
            String zone = inventoryService.getLocationByScope(payload, LocationScope.ZONE).getId();

            if (location.getScope() == LocationScope.REGION) {
                if (location.getId().equals(region)) {
                    regionMatches = true;
                }
            } else if (location.getScope() == LocationScope.ZONE) {
                if (location.getId().equals(zone)) {
                    zoneMatches = true;
                }
            }

            if (!zoneMatches || !regionMatches) {
                location = location.getParent();
            } else {
                break;
            }
        }

        assertTrue(zoneMatches);
        assertTrue(regionMatches);
    }

}
