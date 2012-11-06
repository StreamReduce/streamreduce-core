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

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import com.streamreduce.util.JSONUtils;
import com.streamreduce.util.MessageUtils;
import com.streamreduce.util.Pair;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.junit.Test;

public class NodebellyMessageTransformerTest {
    private static JSONObject metricConfig = null;
    private static String METRIC_CONFIG_JSON;
        
    @Test
    public void testReadAndParseConfigFile() throws Exception {
        
        METRIC_CONFIG_JSON = JSONUtils.readJSONFromClasspath("/metricConfig.json");

        assertTrue(METRIC_CONFIG_JSON.charAt(0) == '{');
        metricConfig = JSONObject.fromObject(METRIC_CONFIG_JSON);
        assertTrue( metricConfig != null );
    }
    
    @Test
    public void testMessageTransFormationSummary() throws Exception {
        testMessageTransFormationOf("NODEBELLY_SUMMARY", 33.4f);
    }
    @Test
    public void testMessageTransFormationStatus() throws Exception {
        testMessageTransFormationOf("NODEBELLY_STATUS", 33.4f);
    }
    @Test
    public void testMessageTransFormationAnomaly() throws Exception {
        testMessageTransFormationOf("NODEBELLY_ANOMALY", 13.4f);
        testMessageTransFormationOf("NODEBELLY_ANOMALY", 23.4f);
        testMessageTransFormationOf("NODEBELLY_ANOMALY", 183.4f);
    }
    @Test
    public void testGetUnits() throws Exception {
        Map<String, String> metricCriteria = new HashMap<String, String>();
        metricCriteria.put("RESOURCE_ID", "DiskReadBytes");
        metricCriteria.put("METRIC_ID", "average");

        NodebellyMessageTransformer transformer = setupTransformer();

        Pair pair = transformer.getUnitsLabel("INVENTORY_ITEM_RESOURCE_USAGE", metricCriteria, 1065.0, true);
        //System.out.println(pair.first + " " + pair.second);
        assert(((String)pair.second).startsWith(" "));
        assert(((String)pair.second).endsWith("Kb"));

        pair = transformer.getUnitsLabel("INVENTORY_ITEM_RESOURCE_USAGE", metricCriteria, 234234235.0, false);
        //System.out.println(pair.first + " " + pair.second);
        assert(!((String)pair.second).startsWith(" "));
        assert(((String)pair.second).endsWith("Mb"));
    }

    private void testMessageTransFormationOf(String mtype, float fvalue) throws Exception {
        ArrayList<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> criteria = new HashMap<String, Object>();
        
        for(int i = 0; i < 3; i++) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("name", "INVENTORY_ITEM_RESOURCE_USAGE");
            criteria.put("RESOURCE_ID", "CPUUtilization");
            criteria.put("METRIC_ID", "average");
            row.put("metricCriteria", criteria);
            row.put("targetConnectionAlias", "ConnectFoo"+i); // ie, Nodeable Cloud
            row.put("targetAlias", "ItemFoo"+i); // ie, i-a35034c7
            row.put("value", new Float(4+i));
            row.put("mean", new Float(43+i));
            row.put("stddev", new Float(0.2f+i));
            row.put("diff", new Float(1.0));
            row.put("min", new Float(-3.0));
            row.put("max", new Float(8.0));
            items.add(row);
        }
        map.put("items", items);
        map.put("account", "4e532e7bb5a8bb09c315a158");
        map.put("timestamp", (new Date()).getTime() );
        map.put("targetProviderId", "4e532e7bb5a8bb09c315a158");
        map.put("name", "INVENTORY_ITEM_RESOURCE_USAGE");
        map.put("metricCriteria", criteria);
        map.put("diff", new Float(8.4));
        map.put("mean", new Float(7.4));
        map.put("stddev", new Float(6.4));
        map.put("total", new Float(13.4));
        map.put("value", new Float(fvalue));
        map.put("granularity", new Long(120000));
        map.put("targetId", new ObjectId("4fa95fc43554603703e727c3"));
        map.put("type", mtype);
        
        map.put("targetConnectionAlias", "ParentConnectionName");
        map.put("targetAlias", "ParentInventoryName");
  
        Event event = new Event();
        event.setEventId(EventId.valueOf((String) map.get("type")));
        event.setTargetId((ObjectId) map.get("targetId"));
        event.setMetadata(map);

        NodebellyMessageTransformer transformer = setupTransformer();
        String output = transformer.doTransform(event);
        assertTrue(output != null);
        //System.out.println(output);
    }

    private NodebellyMessageTransformer setupTransformer() throws Exception {
        Properties messageProperties = new Properties();
        InputStream in = NodebellyMessageTransformerTest.class.getResourceAsStream("/messages.properties");
        messageProperties.load(in);
        in.close();

        JSONObject metricConfig = JSONObject.fromObject(MessageUtils.readJSONFromClasspath("/metricConfig.json"));
        return new NodebellyMessageTransformer(messageProperties, null, metricConfig);
    }
}
