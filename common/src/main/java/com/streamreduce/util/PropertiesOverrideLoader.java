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
        } catch (Exception e) {
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
