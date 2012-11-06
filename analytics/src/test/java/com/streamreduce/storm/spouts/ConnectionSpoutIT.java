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
