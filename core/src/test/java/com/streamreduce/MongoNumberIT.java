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

package com.streamreduce;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.code.morphia.Datastore;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.util.JSON;
import com.streamreduce.core.dao.DAODatasourceType;
import com.streamreduce.core.dao.GenericCollectionDAO;

public class MongoNumberIT extends AbstractServiceTestCase {

    @Autowired
    @Qualifier(value = "businessDBDatastore")
    private Datastore businessDatastore;
    
    @Autowired
    private GenericCollectionDAO gcdao;

    @Test
    @Ignore("Integration Tests depended on sensitive account keys, ignoring until better harness is in place.")
    public void testNumberFidelity() {
        String json = "{\"min\":" + Constants.PERIOD_MINUTE
            + ",\"hour\":" + Constants.PERIOD_HOUR
            + ",\"day\":" + Constants.PERIOD_DAY
            + ",\"week\":" + Constants.PERIOD_WEEK
            + ",\"month\":" + Constants.PERIOD_MONTH
            + ",\"crap\":" + Constants.PERIOD_WEEK * 30
            + ",\"foo\":\"foo\"}";
        
        BasicDBObject newPayloadObject = (BasicDBObject) JSON.parse(json);
        gcdao.createCollectionEntry(DAODatasourceType.BUSINESS, getClass().toString(), newPayloadObject);
        
        DB db = businessDatastore.getDB();
        DBCollection collection = db.getCollection(getClass().toString());
        BasicDBObject results = (BasicDBObject) collection.findOne(new BasicDBObject("foo", "foo"));
        
        assertTrue( ((Integer) results.get("min")).longValue() == Constants.PERIOD_MINUTE);
        assertTrue( ((Integer) results.get("hour")).longValue() == Constants.PERIOD_HOUR);
        assertTrue( ((Integer) results.get("day")).longValue() == Constants.PERIOD_DAY);
        assertTrue( ((Integer) results.get("week")).longValue() == Constants.PERIOD_WEEK);
        assertTrue( (Long) results.get("crap") == Constants.PERIOD_WEEK * 30);
        assertTrue( (Long) results.get("month") == Constants.PERIOD_MONTH);
        
        String valMin = results.get("min").toString();
        String valHour = results.get("hour").toString();
        String valDay = results.get("day").toString();
        String valWeek = results.get("week").toString();
        String valMonth = results.get("month").toString();
        String valCrap = results.get("crap").toString();
        
        System.out.println("valMin: " + valMin);
        System.out.println("valHour: " + valHour);
        System.out.println("valDay: " + valDay);
        System.out.println("valWeek: " + valWeek);
        System.out.println("valMonth: " + valMonth);
        System.out.println("valCrap: " + valCrap);
        
        assertTrue( Long.valueOf(valMin) == Constants.PERIOD_MINUTE);
        assertTrue( Long.valueOf(valHour) == Constants.PERIOD_HOUR);
        assertTrue( Long.valueOf(valDay) == Constants.PERIOD_DAY);
        assertTrue( Long.valueOf(valWeek) == Constants.PERIOD_WEEK);
        assertTrue( Long.valueOf(valMonth) == Constants.PERIOD_MONTH);
        assertTrue( Long.valueOf(valCrap) == Constants.PERIOD_WEEK * 30);
        
    }
}
