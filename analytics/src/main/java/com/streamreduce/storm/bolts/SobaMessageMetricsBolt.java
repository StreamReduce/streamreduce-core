package com.streamreduce.storm.bolts;

import java.util.Map;

import com.streamreduce.core.event.EventId;

/**
 * Extension of {@link AbstractMetricsBolt} that handles specialized events for the SobaMessage stream.
 */
public class SobaMessageMetricsBolt extends AbstractMetricsBolt {

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(String id, Long timestamp, EventId eventId, String accountId, String userId,
                            String targetId, Map<String, Object> metadata) {
        // No specialized event handling necessary for 'Account' events and is here purely to "plug in" to the
        // built-in metric handling
    }

}
