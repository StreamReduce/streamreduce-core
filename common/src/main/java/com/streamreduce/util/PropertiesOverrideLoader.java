package com.streamreduce.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class that loads Properties and applies overrides to them if override.properties exists on the classpath.
 */
public class PropertiesOverrideLoader {
    private static final String OVERRIDE_PROPERTIES_FILENAME = "/override.properties";

    private static final Logger LOGGER = Logger.getLogger(PropertiesOverrideLoader.class);


    public static Properties loadProperties(String propertiesPath) {
        if (!propertiesPath.endsWith(".properties")) {
            propertiesPath = propertiesPath + ".properties";
        }
        if (!propertiesPath.startsWith("/")) {
            propertiesPath = "/" + propertiesPath;
        }

        Properties actualProperties = new Properties();

        try {
            InputStream propsInputStream = PropertiesOverrideLoader.class.getResourceAsStream(propertiesPath);
            actualProperties.load(propsInputStream);
        } catch (IOException e) {
            LOGGER.error("Unable to load " + propertiesPath + " from classpath.", e);
        }

        try {
            Properties overrideProperties = new Properties();
            InputStream propsInputStream = PropertiesOverrideLoader.class.getResourceAsStream(OVERRIDE_PROPERTIES_FILENAME);
            overrideProperties.load(propsInputStream);

            for (String key : actualProperties.stringPropertyNames()) {
                if (overrideProperties.containsKey(key)) {
                    actualProperties.setProperty(key, overrideProperties.getProperty(key));
                    LOGGER.info("Property of " + key + " in " + propertiesPath + " has been overriden from " +
                            OVERRIDE_PROPERTIES_FILENAME);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Unable to load " + OVERRIDE_PROPERTIES_FILENAME + " from the classpath.  " +
                    "Default properties for " + propertiesPath + " will be used.  Reason:  " +
                    (e.getMessage() != null ? e.getMessage() : ""));
        }

        return actualProperties;
    }
}
