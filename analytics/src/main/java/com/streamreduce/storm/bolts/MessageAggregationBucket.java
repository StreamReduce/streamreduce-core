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