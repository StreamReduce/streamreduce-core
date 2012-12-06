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

package com.streamreduce.core.transformer.message;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/13/12 10:01 AM</p>
 */
public class ConnectionMessageTransformerTest {

    private Properties properties;
    private Map<String, Object> metadata;

    @Before
    public void setUp() throws Exception {
        ResourceBundle messagesBundle = ResourceBundle.getBundle("messages");
        properties = new Properties();
        for (String key : messagesBundle.keySet()) {
            properties.put(key, messagesBundle.getString(key));
        }

        metadata = new HashMap<>();
        metadata.put("sourceAlias", "sourceAlias");
        metadata.put("targetProviderDisplayName", "targetProviderDisplayName");
        metadata.put("targetAlias", "targetAlias");
    }

    @Test
    public void testCreatedTransform() throws Exception {
        testDoTransform(EventId.CREATE, "@sourceAlias has created a targetProviderDisplayName connection: targetAlias");
    }

    @Test
    public void testUpdatedTransform() throws Exception {
        testDoTransform(EventId.UPDATE, "@sourceAlias has updated a targetProviderDisplayName connection: targetAlias");
    }

    @Test
    public void testDeletedTransform() throws Exception {
        testDoTransform(EventId.DELETE, "@sourceAlias has deleted a targetProviderDisplayName connection: targetAlias");
    }

    private void testDoTransform(EventId eventId, String expected) throws Exception {
        Event event = new Event.Builder().eventId(eventId).context(metadata).build();
        ConnectionMessageTransformer transformer = new ConnectionMessageTransformer(properties, null);
        Assert.assertEquals(expected, transformer.doTransform(event));
    }

}
