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
