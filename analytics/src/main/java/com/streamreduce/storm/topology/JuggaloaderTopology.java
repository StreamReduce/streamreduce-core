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

package com.streamreduce.storm.topology;


import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import com.streamreduce.Constants;
import com.streamreduce.storm.GroupingNameConstants;
import com.streamreduce.storm.bolts.AccountMetricsBolt;
import com.streamreduce.storm.bolts.ConnectionMetricsBolt;
import com.streamreduce.storm.bolts.InventoryItemMetricsBolt;
import com.streamreduce.storm.bolts.JuggaloaderMessageGeneratorBolt;
import com.streamreduce.storm.bolts.JuggaloaderTimeBaseBolt;
import com.streamreduce.storm.bolts.PersistMetricsBolt;
import com.streamreduce.storm.bolts.SobaMessageMetricsBolt;
import com.streamreduce.storm.bolts.UserMetricsBolt;
import com.streamreduce.storm.spouts.EventSpout;
import com.streamreduce.storm.spouts.JuggaloaderCommandSpout;


public class JuggaloaderTopology {

    public StormTopology createJuggaloaderTopology() {
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("eventSpout", new EventSpout());
        builder.setSpout("commandSpout", new JuggaloaderCommandSpout());
        builder.setBolt("accountMetricsBolt", new AccountMetricsBolt())
                .shuffleGrouping("eventSpout", GroupingNameConstants.ACCOUNT_GROUPING_NAME);
        builder.setBolt("connectionMetricsBolt", new ConnectionMetricsBolt())
                .shuffleGrouping("eventSpout", GroupingNameConstants.CONNECTION_GROUPING_NAME);
        builder.setBolt("inventoryItemMetricsBolt", new InventoryItemMetricsBolt())
                .shuffleGrouping("eventSpout", GroupingNameConstants.INVENTORY_ITEM_GROUPING_NAME);
        builder.setBolt("userMetricsBolt", new UserMetricsBolt())
                .shuffleGrouping("eventSpout", GroupingNameConstants.USER_GROUPING_NAME);
        builder.setBolt("messageMetricsBolt", new SobaMessageMetricsBolt())
               .shuffleGrouping("eventSpout", GroupingNameConstants.MESSAGE_GROUPING_NAME);
        builder.setBolt("second", new JuggaloaderTimeBaseBolt(0))
                .fieldsGrouping("accountMetricsBolt", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("connectionMetricsBolt", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("inventoryItemMetricsBolt", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("userMetricsBolt", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("messageMetricsBolt", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("commandSpout", new Fields("metricAccount", "metricName"));
        builder.setBolt("minute", new JuggaloaderTimeBaseBolt(Constants.PERIOD_MINUTE))
                .fieldsGrouping("second", new Fields("metricAccount", "metricName"));
        builder.setBolt("hour", new JuggaloaderTimeBaseBolt(Constants.PERIOD_HOUR))
                .fieldsGrouping("minute", new Fields("metricAccount", "metricName"));
        builder.setBolt("day", new JuggaloaderTimeBaseBolt(Constants.PERIOD_DAY))
                .fieldsGrouping("hour", new Fields("metricAccount", "metricName"));
        builder.setBolt("week", new JuggaloaderTimeBaseBolt(Constants.PERIOD_WEEK))
                .fieldsGrouping("day", new Fields("metricAccount", "metricName"));
        builder.setBolt("month", new JuggaloaderTimeBaseBolt(Constants.PERIOD_MONTH))
                .fieldsGrouping("week", new Fields("metricAccount", "metricName"));
        builder.setBolt("persistence", new PersistMetricsBolt())
                .shuffleGrouping("minute")
                .shuffleGrouping("hour")
                .shuffleGrouping("day")
                .shuffleGrouping("week")
                .shuffleGrouping("month");
        // builder.setBolt("message", new JuggaloaderAnomalyGeneratorBolt()) // TODO - replace the next line with this one when SOBA-1521 is done
        builder.setBolt("message", new JuggaloaderMessageGeneratorBolt())
                .fieldsGrouping("second", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("minute", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("hour", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("day", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("week", new Fields("metricAccount", "metricName"))
                .fieldsGrouping("month", new Fields("metricAccount", "metricName"));
        return builder.createTopology();
    }

}
