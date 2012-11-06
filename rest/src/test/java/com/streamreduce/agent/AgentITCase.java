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

package com.streamreduce.agent;

import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.Constants;
import com.streamreduce.connections.CloudProvider;
import com.streamreduce.core.dao.SobaMessageDAO;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.ConnectionService;
import com.streamreduce.core.service.InventoryService;
import com.streamreduce.core.service.UserService;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AgentITCase extends AbstractInContainerTestCase {

    private static String AGENT_URL = "http://localhost:6099/agent/metrics";
    private static String AGENT_TEST_MESSAGE = "com/nodeable/agent/agent-test-message.json";
    private static String AGENT_TEST_MESSAGE_NODATA = "com/nodeable/agent/agent-nodata-test-message.json";
    private static String AGENT_TEST_MESSAGE_NOINVENTORYID = "com/nodeable/agent/agent-noinventoryid-test-message.json";
    private static String AGENT_TEST_MESSAGE_INVALIDINVENTORYID = "com/nodeable/agent/agent-invalidinventoryid-test-message.json";

    @Autowired
    private UserService userService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private SobaMessageDAO sobaMessageDAO;

    private InventoryItem inventoryItem;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // we know this user has inventory items right now...
        // just grab the first one we find.
        User user = userService.getUser(Constants.NODEABLE_SUPER_USERNAME);
        Connection cloud = connectionService.getConnections(CloudProvider.TYPE, user).get(0);
        List<InventoryItem> inventoryItems = inventoryService.getInventoryItems(cloud);
        int retry = 0;

        while (inventoryItems.size() == 0 && retry < 3) {
            Thread.sleep(30000);
            inventoryItems = inventoryService.getInventoryItems(cloud);
            retry++;
        }

        if (inventoryItems.size() == 0) {
            throw new Exception("Unable to prepare for the test so tests are unable to run.");
        }

        inventoryItem = inventoryItems.get(0);

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }


    @Test
    @Ignore
    public void testAgentMessage() throws HttpException, IOException {
        sendAgentMessage(AGENT_TEST_MESSAGE, 201);

        // Assert that 1 SobaMessages of type AGENT are inserted into "messages"
        // collection.

        //Commented out until we have a better way to test other than using an AdvancedDataStore reference

//        assertEquals(1,
//                     messageDatastore.find(MessageUtils.getInboxPath(inventoryItem.getConnection().getUser()),
//                                           SobaMessage.class).filter("type", MessageType.AGENT).countAll());
    }

    @Test
    @Ignore
    public void testAgentMessageInvalidInventoryId() throws HttpException, IOException {
        sendAgentMessage(AGENT_TEST_MESSAGE_INVALIDINVENTORYID, 400);

        // Assert that 0 SobaMessages of type AGENT are inserted into "messages"
        // collection.

        //Commented out until we have a better way to test other than using an AdvancedDataStore reference

//        assertEquals(0,
//                     messageDatastore.find(MessageUtils.getInboxPath(inventoryItem.getConnection().getUser()),
//                                           SobaMessage.class).filter("type", MessageType.AGENT).countAll());

    }

    @Test
    @Ignore
    public void testAgentMessageNoData() throws HttpException, IOException {
        sendAgentMessage(AGENT_TEST_MESSAGE_NODATA, 400);

        // Assert that 0 SobaMessages of type AGENT are inserted into "messages"
        // collection.

        //Commented out until we have a better way to test other than using an AdvancedDataStore reference

//        assertEquals(0,
//                     messageDatastore.find(MessageUtils.getInboxPath(inventoryItem.getConnection().getUser()),
//                                           SobaMessage.class).filter("type", MessageType.AGENT).countAll());

    }

    @Test
    @Ignore
    public void testAgentMessageNoInventoryId() throws HttpException, IOException {
        sendAgentMessage(AGENT_TEST_MESSAGE_NOINVENTORYID, 400);

        // Assert that 0 SobaMessages of type AGENT are inserted into "messages"
        // collection.

        //Commented out until we have a better way to test other than using an AdvancedDataStore reference

//        assertEquals(0,
//                     messageDatastore.find(MessageUtils.getInboxPath(inventoryItem.getConnection().getUser()),
//                                           SobaMessage.class).filter("type", MessageType.AGENT).countAll());

    }

    protected void sendAgentMessage(String agentMessage, int expectedStatusCode) throws IOException {
        // Post test agent message to agent url
        HttpClient client = new DefaultHttpClient();
        HttpPost postMethod = new HttpPost(AGENT_URL);

        InputStream stream = AgentITCase.class.getResourceAsStream(agentMessage);

        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, "UTF-8");
        String payload = injectValidNodeId(writer.toString());

        postMethod.setEntity(new StringEntity(payload, MediaType.APPLICATION_JSON));
        HttpResponse response = client.execute(postMethod);
        int status = response.getStatusLine().getStatusCode();

        System.out.println(response.getEntity().getContent());

        // Assert that correct status is returned
        assertEquals(expectedStatusCode, status);
    }

    protected String injectValidNodeId(String agentMessage) {
        return agentMessage.replace("NODE_ID_PLACEHOLDER", inventoryItem.getExternalId());
    }

}
