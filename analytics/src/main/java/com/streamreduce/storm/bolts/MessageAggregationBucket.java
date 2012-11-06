package com.streamreduce.storm.bolts;

import java.util.ArrayList;
import com.streamreduce.Constants;

public class MessageAggregationBucket extends ArrayList<Object> {
    private static final long serialVersionUID = -7354216107723004515L;
    private static final int MAX_LENGTH = 40; // TODO
    private static final long MAX_TIME = Constants.PERIOD_MINUTE * 5;
    private long created;
    String account;
    
    public MessageAggregationBucket(String account) {
        this.account = account;
        resetTime();
    }

    public long getCreated() {
        return created;
    }

    public String getAccount() {
        return account;
    }

    private void resetTime() {        
        created = System.currentTimeMillis();
    }
    
    public boolean isReady() {
        return ( size() >= MAX_LENGTH || ((System.currentTimeMillis() - created) > MAX_TIME));
    }
}