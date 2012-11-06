package com.streamreduce.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;


/**
 * Utility class with a single static method used to format a queue name to be valid and prefixed with an environment
 * identifier in the event we have multiple versions of Nodeable server components using the same AWS account.
 */
public class SqsQueueNameFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsQueueNameFormatter.class);

    private static final int MAX_LENGTH_OF_SQS_QUEUE = 80;

    /**
     * Creates an environmental specific queue name from a desired queue name. The environment prefix will be
     * prepended to original queue name.
     *
     * This method accepts a non-blank queue name and environment as parameters.  Once these two values are joined
     * with a "-" any characters thare are invalid for use in an SQS queue name are replaced transformed with dashes
     * (only alphanumerics, dashes, and underscores are allowed).
     *
     * Additionally, if the passed in environment prefix is "dev" then the prefix will also include the hostname
     * of the machine being used (used for developer workstations using SQS).
     *
     * The final String returned will no exceed the maximum length of a valid SQS name, so any characters above that
     * length will be trimmed off.
     *
     * @param originalQueueName A string representing the desired queue name, before cleanup.
     * @param environmentPrefix A prefix representing an environment that nodeable is deployed to.
     * @return String representing a valid SQS queue name with an environmental prefix prepended to the queue name.
     */
    public static String formatSqsQueueName(String originalQueueName, String environmentPrefix) {
        if (StringUtils.isBlank(originalQueueName) || StringUtils.isBlank(environmentPrefix)) {
            throw new IllegalArgumentException("queueName and environmentPrefix must be non-blank");
        }
        String queueNameWithPrefix = addMachineNameToPrefixIfNeeded(environmentPrefix) + "-" + originalQueueName;
        String modifiedQueueName = queueNameWithPrefix.trim().replaceAll("[^a-zA-Z1-9_-]","-");

        return StringUtils.substring(modifiedQueueName,0,MAX_LENGTH_OF_SQS_QUEUE);
    }

    private static String addMachineNameToPrefixIfNeeded(String environmentPrefix) {
        if ("dev".equals(environmentPrefix.trim())) {
            String devSpecificPrefix;
            try {
                String hostname = InetAddress.getLocalHost().getHostName(); //everyone gets their own dev queues!
                devSpecificPrefix = hostname.split("\\.")[0]; //In case hostname is a FQDN
            } catch (Exception e) {
                LOGGER.warn("Unable to get hostname for dev machine.  Using \"shared\" as a prefix");
                devSpecificPrefix = "shared";
            }
            environmentPrefix = environmentPrefix + "-" + devSpecificPrefix;
        }
        return environmentPrefix;
    }
}
