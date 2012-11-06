package com.streamreduce.core.transformer.message;

import com.streamreduce.core.event.EventId;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import com.streamreduce.core.model.messages.details.nodebelly.NodebellyMessageDetails;
import com.streamreduce.core.model.messages.details.nodebelly.NodebellySummaryMessageDetails;
import com.streamreduce.util.MessageUtils;
import com.streamreduce.util.Pair;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This is annoying right now because we have to build both the plain text string and MessageDetails object at the same time
 */
public class NodebellyMessageTransformer extends SobaMessageTransformer implements MessageTransformer {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());
    private JSONObject metricConfig;

    public NodebellyMessageTransformer(Properties messageProperties, SobaMessageDetails messageDetails, JSONObject metricConfig) {
        super(messageProperties, messageDetails);
        this.metricConfig  = metricConfig;
    }

    /*
     * This helper function grabs a subfield out of the JSON configuration
     * for each metricname->RESOURCE_ID.METRIC_ID in METRIC_CONFIG_JSON.
     */
    private String getMetricConfigValue(String metricName, Map<String, String> metricCriteria, String name) {
        JSONObject item = (JSONObject) metricConfig.get(metricName);
        if (item != null) {
            String key2 = "";
            if (metricCriteria != null && metricCriteria.containsKey("RESOURCE_ID") && metricCriteria.containsKey("METRIC_ID")) {
                key2 = metricCriteria.get("RESOURCE_ID") + "." + metricCriteria.get("METRIC_ID");
            }
            item = (JSONObject) item.get(key2);
            if (item != null) {
                return item.getString(name);
            }
        }
        logger.error("NB: metricNameMapping: miss: getMetricConfigValue:" + metricName + ", " + metricCriteria);
        return null;
    }

    /*
     * Wraps getMetricConfigValue() to return an empty non null string when null.
     */
    private String getMetricConfigValueNotNull(String metricName, Map<String, String> metricCriteria, String name) {
        String value = getMetricConfigValue(metricName, metricCriteria, name);
        if(value == null)
            value = "";
        return value;
    }

    /*
     * Formats the units string according to the metic and its value.
     */
    protected Pair getUnitsLabel(String metricName, Map<String, String> metricCriteria, double value, boolean space) {
        boolean plural = true;
        if (value == 1.0) {
            plural = false;
        }

        String unit = getMetricConfigValueNotNull(metricName, metricCriteria, (plural ? "units" : "unit"));

        /*
         * Divide down the value while stepping through incrementally
         * larger units until it's no longer > 1K of the units.
         */
        if (unit != null && unit.startsWith("Byte")) {
            String prefixes = "KMGTPEZY";
            for(int i = 0; i < prefixes.length(); i++) {
                if (Math.abs(value / 1024.0) > 1.0) {
                    value = value / 1024.0;
                    unit = prefixes.substring(i, i+1) + "b";
                }
            }
        }

        if (space && unit.length() > 0 && !unit.equals("%")) {
            unit = " " + unit;
        }

        return new Pair(value, unit);
    }

    @Override
    public String doTransform(Event event) {

        final EventId eventId = event.getEventId();
        final Map<String, Object> meta = event.getMetadata();
        final Date eventDate = new Date((Long) meta.get("timestamp"));

        String title;
        String details = "";
        
        Map<String, String> topMetricCriteria = (Map<String, String>) meta.get("metricCriteria");

        switch (eventId) {
            case NODEBELLY_STATUS:
            case NODEBELLY_SUMMARY:

                String providerId = (String) meta.get("targetProviderId");

                title = MessageFormat.format((String) messageProperties.get("message.nodebelly.summary"),
                        eventDate,
                        providerId);

                // not all status/summary messages have been aggregated
                if (meta.containsKey("items")) {

                    // make the names of each item human readable
                    List<Map<String, Object>> items = (List<Map<String, Object>>) meta.get("items");
                    boolean first = true;
                    for (Map<String, Object> item : items) {
                        Map<String, String> metricCriteria = (Map<String, String>) item.get("metricCriteria");
                        String metricName = (String) item.get("name");
                        float origValue = ((Number) item.get("value")).floatValue();
                        float origDiff = ((Number) item.get("diff")).floatValue();

                        Pair pair = getUnitsLabel(metricName, metricCriteria, ((Number) item.get("value")).doubleValue(), true);
                        item.put("value", ((Number)pair.first).floatValue());

                        Pair pairDiff = getUnitsLabel(metricName, metricCriteria, ((Number) item.get("diff")).doubleValue(), true);
                        item.put("diff", ((Number)pairDiff.first).floatValue());

                        /*
                        * Just render an explanation subheader for the first item since it
                        * has the highest stddev and will be selected by the client
                        */
                        if (first) {
                            first = false;
                            String explanation = getMetricConfigValueNotNull(metricName, metricCriteria, "explanation");
                            double previous = origValue - origDiff;
                            if (metricName.equals("CONNECTION_RESOURCE_USAGE") && metricCriteria.containsKey("RESOURCE_ID")) { // TODO hack for IMG
                                explanation = metricCriteria.get("RESOURCE_ID") + " was previously at %.2f and is now at %.2f";
                            }
                            if (explanation.length() > 0) {
                                explanation = MessageFormat.format(explanation, String.valueOf(previous), String.valueOf(pair.first) );
                            }
                            item.put("subheader", explanation);
                        }

                        item.put("name", metricTypeNameReadable(metricName, metricCriteria));
                        item.put("metricname", metricName); // needed for debugging
                        item.put("unit", pair.second);
                    }

                    HashMap<String, Object> structure = new HashMap<String, Object>();
                    structure.put("accountId", meta.get("account"));
                    structure.put("total", meta.get("total"));
                    structure.put("diff", meta.get("diff"));
                    structure.put("type", providerId);
                    structure.put("items", items); // add the updated items
                    structure.put("granularity", meta.get("granularity"));

                    // set rich formatting properties
                    // the client will render the table in "structure" how it wants to
                    messageDetails = new NodebellySummaryMessageDetails.Builder()
                            .title(title)
                            .structure(structure)
                            .build();

                    // just print the key/value pairs for the plain text version
                    details = ((NodebellySummaryMessageDetails) messageDetails).toPlainText();
                }

                break;

            case NODEBELLY_ANOMALY:

                String connectionName = "";
                if (meta.containsKey("targetConnectionAlias")) {
                    connectionName = ((String) meta.get("targetConnectionAlias"));
                }
                
                String inventoryName = "";
                if (meta.containsKey("targetAlias")) {
                    inventoryName =  " for " + ((String) meta.get("targetAlias"));
                }
                
                providerId = (String) meta.get("targetProviderId");
                String metricName = (String) meta.get("name");
                float fValue = ((Number) meta.get("value")).floatValue();
                float fMean = ((Number) meta.get("mean")).floatValue();
                float fStddev = ((Number) meta.get("stddev")).floatValue();

                int nStdDev = Double.valueOf(Math.floor(Math.abs(fValue - fMean) / fStddev)).intValue();

                Pair pair1 = getUnitsLabel(metricName, topMetricCriteria, fValue, true);
                //Pair pair2 = getUnitsLabel(metricName, topMetricCriteria, fStddev, true);
                Pair pair3 = getUnitsLabel(metricName, topMetricCriteria, fMean, true);

                int severity = getSeverityLevel(fValue, fMean, fStddev);
                title = MessageFormat.format((String) messageProperties.get("message.nodebelly.anomalyseverity" + severity),
                        metricTypeNameReadable(metricName, topMetricCriteria),
                        connectionName);

                String numericDetails = MessageFormat.format((String) messageProperties.get("message.nodebelly.anomaly.numericdetails"),
                        MessageUtils.roundAndTruncate(((Number) pair1.first).doubleValue(), 2),
                        pair1.second,
                        nStdDev,
                        MessageUtils.roundAndTruncate(((Number) pair3.first).doubleValue(), 2),
                        pair3.second);

                if (nStdDev > 25) {
                    numericDetails = MessageFormat.format((String) messageProperties.get("message.nodebelly.anomaly.numericdetailsBig"),
                            MessageUtils.roundAndTruncate(((Number) pair1.first).doubleValue(), 2),
                            pair1.second,
                            MessageUtils.roundAndTruncate(((Number) pair3.first).doubleValue(), 2),
                            pair3.second);
                }

                details = MessageFormat.format((String) messageProperties.get("message.nodebelly.anomaly.details"),
                        metricTypeNameReadable(metricName, topMetricCriteria),
                        numericDetails,
                        inventoryName);

                HashMap<String, Object> structure = new HashMap<String, Object>();
                structure.put("accountId", meta.get("account"));
                structure.put("name", metricTypeNameReadable(metricName, topMetricCriteria));
                structure.put("metricName", metricName);
                structure.put("metricCriteria", topMetricCriteria);
                structure.put("granularity", meta.get("granularity"));
                structure.put("value", meta.get("value"));
                structure.put("mean", meta.get("mean"));
                structure.put("stddev", meta.get("stddev"));
                structure.put("min", meta.get("min"));
                structure.put("max", meta.get("max"));
                structure.put("diff", meta.get("diff"));
                structure.put("unit", pair1.first);
                
                // set rich formatting properties
                // the client will render the table in "structure" how it wants to
                messageDetails = new NodebellyMessageDetails.Builder()
                        .title(title)
                        .details(details)
                        .structure(structure)
                        .build();

                break;

            default:
                // there really isn't this....
                title = super.doTransform(event);
        }

        // this is the plain text version, the rich message is set in the MessageDetails object
        return title + " " + details;
    }

    /*
     * Used for indexing into the correct headline for anomaly messages.
     * The returned severity is 0, 1 or 2
     */
    private int getSeverityLevel(float value, float mean, float stddev) {
        if(Math.abs(mean - value) > (stddev * 3))
            return 2;
        if(Math.abs(mean - value) > (stddev * 2))
            return 1;
        return 0;
    }

    /*
     * Make friendly names for the following. The rest are being
     * blacklisted by JuggaloaderMessageGeneratorBolt
     * 
     * INVENTORY_ITEM_RESOURCE_USAGE.ID.CPUUtilization.average
     * INVENTORY_ITEM_RESOURCE_USAGE.ID.DiskReadBytes.average
     * INVENTORY_ITEM_RESOURCE_USAGE.ID.DiskReadOps.average
     * INVENTORY_ITEM_RESOURCE_USAGE.ID.WriteReadBytes.average
     * INVENTORY_ITEM_RESOURCE_USAGE.ID.DiskWriteOps.average
     * INVENTORY_ITEM_RESOURCE_USAGE.ID.NetworkIn.average
     * INVENTORY_ITEM_RESOURCE_USAGE.ID.NetworkOut.average
     * CONNECTION_ACTIVITY_COUNT.ID
     * INVENTORY_ITEM_COUNT.ID.total
     * USER_COUNT
     * 
     */
    private String metricTypeNameReadable(String metricName, Map<String, String> metricCriteria) {
        // IMG Connections
        // TODO how do we handle explanation text and units?
        if (metricName.equals("CONNECTION_ACTIVITY_COUNT") && "gateway".equals(metricCriteria.get("PROVIDER_TYPE")))
            return "Custom Connection Activity";
        if (metricName.equals("CONNECTION_RESOURCE_USAGE") && metricCriteria.containsKey("RESOURCE_ID"))
            return metricCriteria.get("RESOURCE_ID");

        // Appcelerator hack. Don't remove unless you clear it with @NJH.
        if (metricCriteria.containsKey("RESOURCE_ID") && (metricCriteria.get("RESOURCE_ID").startsWith("cloud.") || metricCriteria.get("RESOURCE_ID").startsWith("ti."))) {
            return metricCriteria.get("RESOURCE_ID");
        }

        String nameFromJson = getMetricConfigValue(metricName, metricCriteria, "name");
        if (nameFromJson != null)
            return nameFromJson;

        if (metricName.startsWith("ACCOUNT_COUNT")) {
            metricName = "Account Count";
        } else if (metricName.equals("CONNECTION_ACTIVITY_COUNT") && "rss".equals(metricCriteria.get("PROVIDER_ID"))) {
            metricName = "RSS Activity";
        } else if (metricName.equals("CONNECTION_ACTIVITY_COUNT") && "github".equals(metricCriteria.get("PROVIDER_ID"))) {
            metricName = "Github Activity";
        } else if (metricName.equals("CONNECTION_ACTIVITY_COUNT") && "jira".equals(metricCriteria.get("PROVIDER_ID"))) {
            metricName = "Jira Activity";
        } else if (metricName.equals("CONNECTION_ACTIVITY_COUNT") && "aws".equals(metricCriteria.get("PROVIDER_ID"))) {
            metricName = "AWS Activity";
        } else if (metricName.equals("CONNECTION_ACTIVITY_COUNT") && metricCriteria.containsKey("CONNECTION_ID")) {
            metricName = "Connection Activity";
        } else if (metricName.equals("CONNECTION_ACTIVITY_COUNT")) {
            metricName = "Account Level Connection Activity";
        } else if (metricName.equals("CONNECTION_COUNT")) {
            metricName = "Connection Count";
        } else if (metricName.equals("INVENTORY_ITEM_COUNT")) {
            metricName = "Inventory Item Count";
        } else if (metricName.equals("INVENTORY_ITEM_ACTIVITY_COUNT")) {
            metricName = "Inventory Activity Count";
        } else if (metricName.equals("PENDING_USER_COUNT")) {
            metricName = "Pending User Count";
        } else if (metricName.equals("USER_COUNT")) {
            metricName = "User Count";
        }
        logger.error("NB: metricNameMapping: miss: metricTypeNameReadable:" + metricName + ", " + metricCriteria);
        return metricName;
    }

}
