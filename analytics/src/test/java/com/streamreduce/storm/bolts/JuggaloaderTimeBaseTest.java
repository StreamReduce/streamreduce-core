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

package com.streamreduce.storm.bolts;

import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.TupleImpl;
import backtype.storm.tuple.Values;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import com.streamreduce.Constants;
import com.streamreduce.analytics.MetricName;
import com.streamreduce.storm.JuggaloaderStreamState;
import com.streamreduce.util.JSONUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the behavior of the JuggaloaderTimeBase is consistent with an external model.
 *
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 6/26/12 1:26 PM</p>
 */
public class JuggaloaderTimeBaseTest {

    private static final double TOLERANCE = 0.0001;
    private static DateTime dateTime = new DateTime();

    @Test
    public void testProcess_function0() throws Exception {
        testProcess("function0");
    }

    @Test
    public void testProcess_function1() throws Exception {
        testProcess("function1");
    }

    @Test
    public void testProcess_function2() throws Exception {
        testProcess("function2");
    }

    @Test
    public void testProcess_function3() throws Exception {
        testProcess("function3");
    }

    @Test
    public void testProcess_function4() throws Exception {
        testProcess("function4");
    }

    @Test
    public void testProcess_function5() throws Exception {
        testProcess("function5");
    }

    @Test
    public void testProcess_function6() throws Exception {
        testProcess("function6");
    }

    @Test
    public void testProcess_function7() throws Exception {
        testProcess("function7");
    }

    /*
    @Test
    public void testProcess_function8() throws Exception {
        testProcess("function8");
    }
    */

    private void testProcess(String testFunction) throws Exception {
        BasicDBList testData = getTestData(testFunction);
        Iterator<Object> iter = testData.iterator();
        Map<String, JuggaloaderStreamState> states = new HashMap<String, JuggaloaderStreamState>();
        Values previousValues = null;
        for (int i = 0; iter.hasNext(); i++) {
            BasicDBObject dbObject = (BasicDBObject) iter.next();
            dbObject.put("testFunction", testFunction);
            Tuple tuple = createTuple(dbObject);
            Values values = JuggaloaderTimeBase.process(tuple, states, Constants.PERIOD_MINUTE, null);
            assertNotNull(values);

            if (i > JuggaloaderTimeBase.W) {
                if (dbObject.getString("isanomaly").equals("A") && !((Boolean)values.get(13))) {
                    fail(testFunction, true, dbObject, getStreamState(states), values, previousValues);
                }
                /* check for a false positive */
                else if (!dbObject.getString("isanomaly").equals("A") && ((Boolean)values.get(13))) {
                    fail(testFunction, false, dbObject, getStreamState(states), values, previousValues);
                }
                else {
                    /* set anomalyReset on the state object to 0 to avoid the SNOOZE value. */
                    getStreamState(states).anomalyReset = 0;
                    previousValues = values;
                }
            }

            /* test that difference between numerical values in input and calculated by JL are within tolerance */
            /*
            if (Math.abs(Double.parseDouble(dbObject.getString("y")) - (Float)values.get(4)) > TOLERANCE) {
                Assert.fail("Tolerance check: " + dbObject + ", " + values);
            }
            if (Math.abs(Double.parseDouble(dbObject.getString("mean-d")) - (Float)values.get(8)) > TOLERANCE) {
                Assert.fail("Tolerance check: " + dbObject + ", " + values);
            }
            if (Math.abs(Double.parseDouble(dbObject.getString("stddev-d")) - (Float)values.get(9)) > TOLERANCE) {
                Assert.fail("Tolerance check: " + dbObject + ", " + values);
            }
            */
        }
    }

    private Tuple createTuple(BasicDBObject dbObject) {
        Map<String,String> metricCriteria = new HashMap<String,String>();
        metricCriteria.put("one", "1");
        metricCriteria.put("two", "2");

        Tuple tuple = mock(TupleImpl.class);
        when(tuple.getStringByField("metricAccount")).thenReturn("JuggaloaderTimeBaseTest-"+dbObject.getString("testFunction"));
        when(tuple.getStringByField("metricName")).thenReturn(MetricName.TEST_STREAM.toString());
        when(tuple.getStringByField("metricType")).thenReturn("ABSOLUTE");
        when(tuple.getLongByField("metricTimestamp")).thenReturn(getMetricTimestamp());
        when(tuple.getFloatByField("metricValue")).thenReturn(Float.parseFloat(dbObject.getString("y")));
        when(tuple.getValueByField("metaData")).thenReturn(new HashMap<String,Object>());
        when(tuple.getValueByField("metricCriteria")).thenReturn(metricCriteria);
        return tuple;
    }

    private static long getMetricTimestamp() {
        DateTime dt = dateTime.plusMinutes(1);
        long ts = dt.getMillis();
        dateTime = dt;
        return ts;
    }

    private BasicDBList getTestData(String testFunction) throws Exception {
        BasicDBList dbList = (BasicDBList) JSON.parse(
            JSONUtils.readJSONFromClasspath(String.format("/com/streamreduce/storm/bolts/JuggaloaderTimeBaseTest-%s.json", testFunction))
        );
        assert dbList != null;
        return dbList;
    }

    private JuggaloaderStreamState getStreamState(Map<String,JuggaloaderStreamState> states) {
        for (Map.Entry<String,JuggaloaderStreamState> entry : states.entrySet()) {
            return entry.getValue();
        }
        return null;
    }

    private void fail(String functionTest, boolean failedToDetect, BasicDBObject input, JuggaloaderStreamState state, Values values, Values previousValues) {
        String label = failedToDetect ? "Juggaloader failed to detect an anomaly for a test row" : "Juggaloader falsely detected an anomaly for a test row";

        String template = "%s - %s\n[Test Data] row: %s, y: %s, mean: %s, stddev-d: %s, isanomaly: %s\n" +
                "[State Data] yAvgLast: %s, yStdDevLast: %s, tslast: %s\n" +
                "[Values Data] y: %s, avgy: %s, stddev: %s, diff: %s, state.min: %s, state.max: %s, anomaly: %s\n"+
                "[Previous Values Data] y: %s, avgy: %s, stddev: %s, diff: %s, state.min: %s, state.max: %s, anomaly: %s\n";
        Assert.fail(String.format(template, label, functionTest,
                input.getString("x"), input.getString("y"), input.getString("mean-d"), input.getString("stddev-d"), input.getString("isanomaly"),
                state.yAvgLast, state.yStdDevLast, state.tslast,
                values.get(4), values.get(8), values.get(9), values.get(10), values.get(11), values.get(12), values.get(13),
                previousValues.get(4), previousValues.get(8), previousValues.get(9), previousValues.get(10), previousValues.get(11), previousValues.get(12), previousValues.get(13)
        ));
    }
}
