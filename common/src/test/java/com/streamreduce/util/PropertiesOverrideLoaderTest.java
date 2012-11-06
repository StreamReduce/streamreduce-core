package com.streamreduce.util;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class PropertiesOverrideLoaderTest {

    @Test
    public void testLoadPropertiesWithoutOverride() throws Exception {
        Properties props = PropertiesOverrideLoader.loadProperties("test");
        String value = props.getProperty("sample.key");
        assertEquals("foo", value);
    }

    @Test
    public void testLoadPropertiesWithOverride() throws Exception {
        Properties props = PropertiesOverrideLoader.loadProperties("test");
        String value = props.getProperty("sample.key.override");
        assertEquals("bar", value);
    }
}
