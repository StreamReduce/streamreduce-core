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

package com.streamreduce.core.transformer.message;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.mongodb.BasicDBObject;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import com.streamreduce.util.MessageUtils;

public class AgentMessageTransformer extends SobaMessageTransformer implements MessageTransformer {

    public AgentMessageTransformer(Properties messageProperties, SobaMessageDetails messageDetails) {
        super(messageProperties, messageDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String doTransform(Event event) {
        EventId eventId = event.getEventId();
        Map<String, Object> eventMetadata = event.getMetadata();
        String msg = "";

        switch (eventId) {
            case ACTIVITY:
                BasicDBObject payload = (BasicDBObject) eventMetadata.get("payload");
                StringBuilder sb = new StringBuilder();

                sb.append("Current system overview at ")
                        .append(eventMetadata.get("activityGenerated")) // Should we format this?
                        .append("\n\n");

                sb.append("Uptime: ")
                  .append(payload.getString("uptime"))
                  .append("s\n")
                  .append("Disk usage:\n");

                BasicDBObject partitionsObj = (BasicDBObject) payload.get("partitions");
                Set<String> partitions = new TreeSet<String>(partitionsObj.keySet());

                for (String key : partitions) {
                    BasicDBObject partition = (BasicDBObject) partitionsObj.get(key);
                    double totalAsKb = partition.getDouble("total");

                    // Certain devices show as 0.00GB and should be pruned
                    if (totalAsKb == 0) {
                        continue;
                    }

                    double totalAsGB = MessageUtils.kbToGB(totalAsKb);
                    double usedAsGB = MessageUtils.kbToGB(partition.getDouble("used"));
                    double freeAsGB = MessageUtils.kbToGB(partition.getDouble("free"));

                    sb.append("  ")
                      .append(key)
                      .append(": Total ")
                      .append(MessageUtils.roundAndTruncate(totalAsGB, 2))
                      .append("GB, Used ")
                      .append(MessageUtils.roundAndTruncate(usedAsGB, 2))
                      .append("GB, Free ")
                      .append(MessageUtils.roundAndTruncate(freeAsGB, 2))
                      .append("GB\n");
                }

                sb.append("Disk I/O:\n");

                BasicDBObject diskIO = (BasicDBObject) payload.get("disk_io");
                Set<String> disks = new TreeSet<String>(diskIO.keySet());

                if (disks.size() == 0) {
                    sb.append("  Unavailable\n");
                } else {
                    for (String key : disks) {
                        BasicDBObject disk = (BasicDBObject) diskIO.get(key);
                        long reads = disk.getLong("read_count");
                        long writes = disk.getLong("write_count");
                        double gbRead = MessageUtils.kbToGB(disk.getLong("read_kbytes"));
                        double gbWrite = MessageUtils.kbToGB(disk.getLong("write_kbytes"));
                        long readSecs = disk.getLong("read_time");
                        long writeSecs = disk.getLong("write_time");

                        sb.append("  ")
                          .append(key)
                          .append(": Reads ")
                          .append(reads)
                          .append(", Writes ")
                          .append(writes)
                          .append(", GB Read ")
                          .append(MessageUtils.roundAndTruncate(gbRead, 2))
                          .append(", GB Written ")
                          .append(MessageUtils.roundAndTruncate(gbWrite, 2))
                          .append(", Read Time ")
                          .append(readSecs)
                          .append("s, Write Time ")
                          .append(writeSecs)
                          .append("s\n");
                    }
                }

                sb.append("Network I/O:\n");

                BasicDBObject netIO = (BasicDBObject) payload.get("network_io");
                Set<String> nics = new TreeSet<String>(netIO.keySet());
                int nicsDisplayed = 0;

                for (String key : nics) {
                    BasicDBObject nic = (BasicDBObject) netIO.get(key);
                    long packetsIn = nic.getInt("packets_in");
                    long packetsOut = nic.getInt("packets_out");

                    // Certain devices show 0 packets in/out and should be pruned
                    if (packetsIn == 0 && packetsOut == 0) {
                        continue;
                    }

                    double gbIn = MessageUtils.kbToGB(nic.getLong("kbytes_in"));
                    double gbOut = MessageUtils.kbToGB(nic.getLong("kbytes_out"));

                    sb.append("  ")
                      .append(key)
                      .append(": Packets In ")
                      .append(packetsIn)
                      .append(", Packets Out ")
                      .append(packetsOut)
                      .append(", GB In ")
                      .append(MessageUtils.roundAndTruncate(gbIn, 2))
                      .append(", GB Out ")
                      .append(MessageUtils.roundAndTruncate(gbOut, 2))
                      .append("\n");

                    nicsDisplayed++;
                }

                if (nicsDisplayed == 0) {
                    sb.append("  Unavailable\n");
                }

                sb.append("Load: 1m ")
                  .append(MessageUtils.roundAndTruncate(payload.getDouble("load_avg_0"), 2))
                  .append(", ")
                  .append("5m ")
                  .append(MessageUtils.roundAndTruncate(payload.getDouble("load_avg_1"), 2))
                  .append(", ")
                  .append("15m ")
                  .append(MessageUtils.roundAndTruncate(payload.getDouble("load_avg_2"), 2))
                  .append("\n");


                float gbTotalRAM = (float) MessageUtils.kbToGB(payload.getLong("phy_ram_total"));
                float gbUsedRAM = (float) MessageUtils.kbToGB(payload.getLong("phy_ram_used"));
                float gbFreeRAM = (float) MessageUtils.kbToGB(payload.getLong("phy_ram_free"));

                sb.append("Real Mem: Total ")
                  .append(MessageUtils.roundAndTruncate(gbTotalRAM, 2))
                  .append("GB, Used ")
                  .append(MessageUtils.roundAndTruncate(gbUsedRAM, 2))
                  .append("GB, Free ")
                  .append(MessageUtils.roundAndTruncate(gbFreeRAM, 2))
                  .append("GB\n");

                double gbTotalVRAM = MessageUtils.kbToGB(payload.getLong("vir_ram_total"));
                double gbUsedVRAM = MessageUtils.kbToGB(payload.getLong("vir_ram_used"));
                double gbFreeVRAM = MessageUtils.kbToGB(payload.getLong("vir_ram_free"));

                sb.append("Virt Mem: Total ")
                  .append(MessageUtils.roundAndTruncate(gbTotalVRAM, 2))
                  .append("GB, Used ")
                  .append(MessageUtils.roundAndTruncate(gbUsedVRAM, 2))
                  .append("GB, Free ")
                  .append(MessageUtils.roundAndTruncate(gbFreeVRAM, 2))
                  .append("GB\n");

                sb.append("Processes: ")
                  .append(payload.getInt("processes"))
                  .append("\n");

                sb.append("Users: Total ")
                  .append(payload.getInt("users_total"))
                  .append(", Unique ")
                  .append(payload.getInt("users_unique"))
                  .append("\n");

                msg = sb.toString();
                break;
            default:
                super.doTransform(event);
                break;
        }
        return msg;
    }

}
