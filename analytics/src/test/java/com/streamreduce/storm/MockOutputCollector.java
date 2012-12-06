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

package com.streamreduce.storm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtype.storm.spout.ISpoutOutputCollector;
import backtype.storm.task.IOutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * MockOutputCollector implements both {@link IOutputCollector} and {@link ISpoutOutputCollector}
 * for testing purposes only.
 */
public class MockOutputCollector implements IOutputCollector, ISpoutOutputCollector {

    private List<Tuple> ackedTuples = new ArrayList<>();
    private Map<String, List<Values>> emittedValuesMap = new HashMap<>();
    private Map<String, List<Values>> emittedSpoutValuesMap = new HashMap<>();
    private Values lastEmmitedValues;

    public Map<String, List<Values>> getEmittedValuesMap() {
        return emittedValuesMap;
    }

    public Map<String, List<Values>> getEmittedSpoutValuesMap() {
        return emittedSpoutValuesMap;
    }

    /* Helper methods for non-spout output collection */

    /**
     * Returns the list of values emitted for the stream specified.
     *
     * @param streamId the stream whose values we're interested in
     *
     * @return the list of values emitted to the specific stream to this collector from a non-spout
     */
    public List<Values> getEmittedValuesForStream(String streamId) {
        return emittedValuesMap.get(streamId);
    }

    /**
     * Returns the list of values emitted for all streams from a non-spout.
     *
     * @return the list of values emitted to this collector from a non-spout
     */
    public List<Values> getEmittedValues() {
        List<Values> allValues = new ArrayList<>();

        for (Map.Entry<String, List<Values>> entry : emittedValuesMap.entrySet()) {
            allValues.addAll(entry.getValue());
        }

        return allValues;
    }

    /**
     * Returns the last emmited value.
     * @return the last emmited value.
     */
    public Values getLastEmmitedValue() {
        return lastEmmitedValues;
    }

    /**
     * Resets the emitted values.
     */
    public void clearEmittedValues() {
        emittedValuesMap.clear();
        emittedValuesMap = new HashMap<>();
    }

    /* Helper methods for spout output collection */

    /**
     * Returns the list of values emitted for the stream specified by the spout.
     *
     * @param streamId the stream whose values we're interested in
     *
     * @return the list of values emitted to the specific stream to this collector from a spout
     */
    public List<Values> getEmittedSpoutValuesForStream(String streamId) {
        return emittedValuesMap.get(streamId);
    }

    /**
     * Returns the list of values emitted for all streams by the spout.
     *
     * @return the list of values emitted to this collector from a spout
     */
    public List<Values> getEmittedSpoutValues() {
        List<Values> allValues = new ArrayList<>();

        for (Map.Entry<String, List<Values>> entry : emittedSpoutValuesMap.entrySet()) {
            allValues.addAll(entry.getValue());
        }

        return allValues;
    }

    /**
     * Returns the list of acked tuples.
     *
     * @return the list of acked tuples
     */
    public List<Tuple> getAckedTuples() {
        return ackedTuples;
    }

    /**
     * Resets the emitted spout values.
     */
    public void clearEmittedSpoutValues() {
        emittedSpoutValuesMap.clear();
        emittedSpoutValuesMap = new HashMap<>();
    }

    /* IOutputCollector Methods */

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> emit(String streamId, Collection<Tuple> anchors, List<Object> tuples) {
        if (!emittedValuesMap.containsKey(streamId)) {
            emittedValuesMap.put(streamId, new ArrayList<Values>());
        }

        if (tuples != null) {
            lastEmmitedValues = (Values) tuples;
            emittedValuesMap.get(streamId).add((Values)tuples);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void emitDirect(int taskId, String streamId, Collection<Tuple> anchors, List<Object> tuples) {
        throw new UnsupportedOperationException("MockObjectCollector#emitDirect(int, String, Collection<Tuple>, List<Object>) is not implemented!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ack(Tuple tuple) {
        ackedTuples.add(tuple);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fail(Tuple tuple) {
        throw new UnsupportedOperationException("MockObjectCollector#fail(Tuple) is not implemented!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reportError(Throwable throwable) {
        throw new UnsupportedOperationException("MockObjectCollector#reportError(Throwable) is not implemented!");
    }

    /* ISpoutOutputCollector Methods */

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> emit(String streamId, List<Object> tuple, Object messageId) {
        if (!emittedSpoutValuesMap.containsKey(streamId)) {
            emittedSpoutValuesMap.put(streamId, new ArrayList<Values>());
        }

        if (tuple != null) {
            lastEmmitedValues = (Values) tuple;
            emittedSpoutValuesMap.get(streamId).add((Values) tuple);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void emitDirect(int i, String s, List<Object> objects, Object o) {
        throw new UnsupportedOperationException("MockObjectCollector#emitDirect(int, String, List<Object>, Object) is not implemented!");
    }

}
