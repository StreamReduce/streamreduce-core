package com.streamreduce.connections;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 9/5/12 09:26</p>
 */
public abstract class AbstractAnalyticsProvider extends AbstractConnectionProvider implements AnalyticsProvider {

    @Override
    public String getType() {
        return TYPE;
    }

}
