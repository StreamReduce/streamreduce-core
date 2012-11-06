package com.streamreduce.storm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import com.streamreduce.Constants;
import com.streamreduce.storm.bolts.JuggaloaderMessageGeneratorBolt;
import com.streamreduce.storm.bolts.JuggaloaderPersistenceBolt;
import com.streamreduce.storm.bolts.JuggaloaderTimeBaseBolt;
import com.streamreduce.storm.spouts.JuggaloaderDummySpout;

/**
 * This topology demonstrates Storm's stream groupings and multilang capabilities.
 */
public class JuggaloaderDummyTopology {


    public static void main(String[] args) throws Exception {
        
        TopologyBuilder builder = buildJuggaloaderTopology();

        Config conf = new Config();
        conf.setDebug(true);
        
        if(args!=null && args.length > 0) {
            conf.setNumWorkers(3);
            StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
        } else {        
            conf.setMaxTaskParallelism(3);
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("Juggaloader-dummy", conf, builder.createTopology());
            Thread.sleep(10000);
            cluster.shutdown();
        }
    }
    
    public static TopologyBuilder buildJuggaloaderTopology() {

        // see https://docs.google.com/a/nodeable.com/drawings/d/1ENR-peTco93tLFFau2A5pFi7fqpgTV7ElpSRuQka6lo/edit
        
        int PARALLELIZATION = 8;
        TopologyBuilder builder = new TopologyBuilder();
        Fields fieldsGroupingToTimeBolt = new Fields("metricAccount", "metricName");
        Fields fieldsGroupingToMessageBolt = new Fields("metricAccount", "metricName");
        Fields fieldsGroupingToPersistenceBolt = new Fields("metricAccount", "metricName");
        
        JuggaloaderPersistenceBolt persistenceBolt = new JuggaloaderPersistenceBolt();
        JuggaloaderMessageGeneratorBolt messageGenerator = new JuggaloaderMessageGeneratorBolt();
        
        builder.setSpout("spout", new JuggaloaderDummySpout(), 1);
        
        // spout-->secondsBolt
        builder.setBolt("secondsBolt", new JuggaloaderTimeBaseBolt(0), PARALLELIZATION).fieldsGrouping("spout", fieldsGroupingToTimeBolt);
        
        // secondsBolt-->minuteBolt
        builder.setBolt("minuteBolt", new JuggaloaderTimeBaseBolt(Constants.PERIOD_MINUTE), PARALLELIZATION).fieldsGrouping("secondsBolt", fieldsGroupingToTimeBolt);
        
        // minuteBolt-->hourBolt
        builder.setBolt("hourBolt", new JuggaloaderTimeBaseBolt(Constants.PERIOD_HOUR), PARALLELIZATION).fieldsGrouping("minuteBolt", fieldsGroupingToTimeBolt);

        // hourBolt-->dayBolt
        builder.setBolt("dayBolt", new JuggaloaderTimeBaseBolt(Constants.PERIOD_DAY), PARALLELIZATION).fieldsGrouping("hourBolt", fieldsGroupingToTimeBolt);
        
        // dayBolt-->weekBolt
        builder.setBolt("weekBolt", new JuggaloaderTimeBaseBolt(Constants.PERIOD_WEEK), PARALLELIZATION).fieldsGrouping("dayBolt", fieldsGroupingToTimeBolt);
        
        // weekBolt-->monthBolt
        builder.setBolt("monthBolt", new JuggaloaderTimeBaseBolt(Constants.PERIOD_MONTH), PARALLELIZATION).fieldsGrouping("weekBolt", fieldsGroupingToTimeBolt);
        
        /*
         * Tie the time-bolts to the persistence bolt.
         * 
         * secondsBolt-->persistenceBolt
         * minuteBolt-->persistenceBolt
         * hourBolt-->persistenceBolt
         * dayBolt-->persistenceBolt
         * weekBolt-->persistenceBolt
         * monthBolt-->persistenceBolt
         */
        builder.setBolt("persistenceBolt", persistenceBolt, 1)
            //.fieldsGrouping("secondsBolt", fieldsGroupingToPersistenceBolt) // TODO: this fine grained not really needed
            //.fieldsGrouping("minuteBolt", fieldsGroupingToPersistenceBolt) // TODO: this fine grained not really needed
            .fieldsGrouping("hourBolt", fieldsGroupingToPersistenceBolt)
            .fieldsGrouping("dayBolt", fieldsGroupingToPersistenceBolt)
            .fieldsGrouping("weekBolt", fieldsGroupingToPersistenceBolt)
            .fieldsGrouping("monthBolt", fieldsGroupingToPersistenceBolt);
        
        /*
         * Tie the time-bolts to the message generation bolt.
         * 
         * secondsBolt-->messageGenerator
         * minuteBolt-->messageGenerator
         * hourBolt-->messageGenerator
         * dayBolt-->messageGenerator
         * weekBolt-->messageGenerator
         * monthBolt-->messageGenerator
         */
        builder.setBolt("messageGenerator", messageGenerator, 1)
            .fieldsGrouping("secondsBolt", fieldsGroupingToMessageBolt)
            .fieldsGrouping("minuteBolt", fieldsGroupingToMessageBolt)
            .fieldsGrouping("hourBolt", fieldsGroupingToMessageBolt)
            .fieldsGrouping("dayBolt", fieldsGroupingToMessageBolt)
            .fieldsGrouping("weekBolt", fieldsGroupingToMessageBolt)
            .fieldsGrouping("monthBolt", fieldsGroupingToMessageBolt);
        
        return builder;
    }
}
