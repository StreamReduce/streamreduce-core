package com.streamreduce.storm.spouts;

import com.mongodb.BasicDBObject;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichSpout;

/**
 * Extension of {@link backtype.storm.topology.base.BaseRichSpout} that will
 * populate a {@link Queue} from a database collection, exhaust it completely,
 * wait a period of time and then repopulate.
 */
public abstract class AbstractScheduledDBCollectionSpout extends BaseRichSpout {

    private static final long serialVersionUID = 631732572516649770L;
    private int sleepDuration;
    private Queue<BasicDBObject> dbCollectionQueue = null;
    private Queue<String> failedQueue = null;
    private long sleepStart;
    private SpoutOutputCollector collector;

    /**
     * Constructor.
     *
     * @param sleepDuration the sleep duration in between queue populations
     */
    public AbstractScheduledDBCollectionSpout(int sleepDuration) {
        this.sleepDuration = sleepDuration;
    }

    /**
     * Returns whether or not this spout is in its quiet period.
     *
     * @return true if the spout is in its quiet period and false otherwise
     */
    public boolean isQuiet() {
        return sleepStart != -1;
    }

    /**
     * Returns the queue of connection objects to be processed.
     *
     * @return the connection queue
     */
    public Queue<BasicDBObject> getQueue() {
        return dbCollectionQueue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        collector = spoutOutputCollector;

        buildDBCollectionQueue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nextTuple() {
        BasicDBObject dbCollectionEntry = (!dbCollectionQueue.isEmpty() ? dbCollectionQueue.remove() : null);

        if (dbCollectionEntry == null) {
            if (sleepStart == -1) {
                sleepStart = System.currentTimeMillis();
            } else if ((System.currentTimeMillis() - sleepStart) > sleepDuration) {
                buildDBCollectionQueue();
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            handleDBEntry(collector, dbCollectionEntry);
        }

    }

    @Override
    public void fail(Object msgId) {
        if (failedQueue == null) {
            failedQueue = new LinkedBlockingQueue<String>();
        }
        failedQueue.add(msgId.toString());
    }

    /**
     * Builds the {@link Queue} and adds all entries from {@link #getDBEntries()}.
     */
    private void buildDBCollectionQueue() {
        sleepStart = -1;
        dbCollectionQueue = new LinkedBlockingQueue<BasicDBObject>();

        dbCollectionQueue.addAll(getDBEntries());

        // add failed entries back to the queue
        if (failedQueue != null) {
            while (!failedQueue.isEmpty()) {
                String id = failedQueue.remove();
                BasicDBObject entry = getDBEntry(id);
                if (entry != null) {
                    dbCollectionQueue.add(entry);
                }
            }
        }
    }

    /**
     * Handles the DB collection entry.
     *
     * @param collector the spout's output collector
     * @param entry     the entry to handle
     */
    public abstract void handleDBEntry(SpoutOutputCollector collector, BasicDBObject entry);

    /**
     * Method that builds a {@link Queue} from a MongoDB collection.
     */
    public abstract List<BasicDBObject> getDBEntries();

    /**
     * Method that return a single {@link BasicDBObject}
     *
     * @param id the id of the {@link BasicDBObject} to be returned
     * @return the {@link BasicDBObject}
     */
    public abstract BasicDBObject getDBEntry(String id);

}
