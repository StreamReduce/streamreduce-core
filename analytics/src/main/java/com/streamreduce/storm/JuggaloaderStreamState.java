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

public class JuggaloaderStreamState {

    public int n; // number of samples seen in the stream so far (or in the window (for averaging))
    public float ylast; // the last sample (y-value) of the stream
    public long tslast; // the timestamp of the last sample
    public float yAvgLast; // the last computed running average of the stream
    public float yStdDevLast; // the last computed running standard deviation of the stream
    public float min; // the smallest sample seen so far
    public float max; // the largest sample seen so far
    public long tsLastEmitted; // the last time a sample was emitted by this bolt (worker)
    public byte anomalyReset;

    public JuggaloaderStreamState(float y, long ts) {
        this.ylast = (float) 0.0;
        this.n = 1;
        this.tslast = ts - (long) 1;
        this.yAvgLast = y;
        this.yStdDevLast = (float) 0.0;
        this.min = y;
        this.max = y;
        this.tsLastEmitted = 0;
        this.anomalyReset = 0;
    }

    @Override
    public String toString() {
        return "State: samples: " + this.n +
                ", last y: " + this.ylast +
                ", last ts: " + this.tslast +
                ", last avg: " + this.yAvgLast +
                ", last stddev: " + this.yStdDevLast +
                ", min: " + this.min +
                ", max: " + this.max +
                ", tsLastEmitted: " + this.tsLastEmitted +
                ", anomalyReset: " + this.anomalyReset;
    }
}
