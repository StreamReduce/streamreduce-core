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

package com.streamreduce.storm.spouts;

import backtype.storm.spout.SpoutOutputCollector;
import com.streamreduce.storm.MockOutputCollector;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests that {@link ConnectionSpout} works as expected.
 */
public class ConnectionSpoutIT {

    /**
     * Tests {@link ConnectionSpout} works as expected.  To perform this test,
     * we'll call {@link com.streamreduce.storm.spouts.ConnectionSpout#nextTuple()}
     * just as Storm does and ensure that the number connections emitted are correct.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    @Ignore("This test never finishes.")
    public void testConnectionSpout() throws Exception {
        MockOutputCollector outputCollector = new MockOutputCollector();
        ConnectionSpout spout = new ConnectionSpout();

        spout.open(null, null, new SpoutOutputCollector(outputCollector));

        long connectionCount = spout.getQueue().size();
        boolean complete = false;
        int calls = 0;

        while (!complete) {
            boolean wasSleeping = spout.isQuiet();

            spout.nextTuple();

            // If the spout was sleeping and isn't after nextTuple or vice
            // versa, that means there wasn't an emission from the spout.
            if (!wasSleeping && !spout.isQuiet()) {
                calls++;

                if (calls / connectionCount == 2 && calls % connectionCount == 0) {
                    // Stop testing after cycling through two queues
                    complete = true;
                }
            }

            Assert.assertEquals(calls, outputCollector.getEmittedSpoutValues().size());
        }
    }

}
