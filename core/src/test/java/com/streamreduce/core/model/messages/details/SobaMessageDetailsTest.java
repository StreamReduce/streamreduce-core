package com.streamreduce.core.model.messages.details;


import com.streamreduce.core.model.messages.details.feed.FeedEntryDetails;
import com.streamreduce.core.model.messages.details.jira.JiraActivityDetails;
import com.streamreduce.core.model.messages.details.nodebelly.NodebellyMessageDetails;
import com.streamreduce.core.model.messages.details.nodebelly.NodebellySummaryMessageDetails;
import com.streamreduce.core.model.messages.details.pingdom.PingdomEntryDetails;
import com.streamreduce.core.model.messages.details.twitter.TwitterActivityDetails;
import net.sf.json.JSONObject;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * Test that validates SobaMessageDetails subclasses deserialize correctly from json with annotations on
 * SobaMessageDetails.
 */
public class SobaMessageDetailsTest {

    static ObjectMapper objectMapper;

    @BeforeClass
    public static void beforeClass() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testFeedEntryDetailsDeserializationTest() throws Exception {
        SobaMessageDetails feedEntryDetails = new FeedEntryDetails.Builder()
                .publishedDate(new Date())
                .description("feed message, ya'll")
                .title("sup")
                .url("http://jugga.lo")
                .build();
        Container expected = new Container(feedEntryDetails);
        String json = objectMapper.writeValueAsString(expected);
        Container actual = objectMapper.readValue(json,Container.class);
        assertEquals(expected,actual);
    }

    @Test
    public void testNodebellyMessageDetailsDeserializationTest() throws Exception {
        SobaMessageDetails details = new NodebellyMessageDetails.Builder()
                .structure(new HashMap<String, Object>())
                .title("nodebelly message, y'all")
                .details("anomaly detected, y'all")
                .build();
        Container expected = new Container(details);
        String json = objectMapper.writeValueAsString(expected);
        Container actual = objectMapper.readValue(json,Container.class);
        assertEquals(expected,actual);
    }

    @Test
    public void testNodebellySummaryMessageDetailsDeserializationTest() throws Exception {
       SobaMessageDetails details =   new NodebellySummaryMessageDetails.Builder()
                .structure(new HashMap<String,Object>())
                .title("summary, ya'll")
                .build();
        Container expected = new Container(details);
        String json = objectMapper.writeValueAsString(expected);
        Container actual = objectMapper.readValue(json, Container.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testPingdomEntryDetailsDeserializationTest() throws Exception {
        SobaMessageDetails details =   new PingdomEntryDetails.Builder()
                .checkCreated(1)
                .lastErrorTime(42)
                .lastResponseTime(69)
                .lastTestTime(311)
                .resolution(-1)
                .status("All Good, Y'all")
                .build();
        Container expected = new Container(details);
        String json = objectMapper.writeValueAsString(expected);
        Container actual = objectMapper.readValue(json, Container.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testTwitterActivityDetailsDeserializationTest() throws Exception {
        SobaMessageDetails details =  new TwitterActivityDetails.Builder()
                .favorites(33)
                .followers(1000)
                .friends(0)
                .profile(new JSONObject())
                .statuses(10)
                .build();
        Container expected = new Container(details);
        String json = objectMapper.writeValueAsString(expected);
        Container actual = objectMapper.readValue(json,Container.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testJiraActivityDetailsDeserializationTest() throws Exception {
        SobaMessageDetails details =  new JiraActivityDetails.Builder()
                .html("<head></head>")
                .build();
        Container expected = new Container(details);
        String json = objectMapper.writeValueAsString(expected);
        Container actual = objectMapper.readValue(json, Container.class);
        assertEquals(expected, actual);
    }


    private static class Container {

        public SobaMessageDetails details;

        @SuppressWarnings("unused")
        private Container() {}

        private Container(SobaMessageDetails details) {
            this.details = details;
        }



        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Container container = (Container) o;

            if (details != null ? !details.equals(container.details) : container.details != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return details != null ? details.hashCode() : 0;
        }
    }
}


