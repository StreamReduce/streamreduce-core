package com.streamreduce.storm.bolts;

import java.util.Map;

import com.streamreduce.core.event.EventId;

/**
 * Extension of {@link AbstractMetricsBolt} that handles specialized events for the Account stream.
 */
public class UserMetricsBolt extends AbstractMetricsBolt {

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(String id, Long timestamp, EventId eventId, String accountId, String userId,
                            String targetId, Map<String, Object> metadata) {
        // No specialized event handling necessary for 'User' events and is here purely to "plug in" to the
        // built-in metric handling
    }

}
