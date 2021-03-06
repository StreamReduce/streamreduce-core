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

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class SqsQueueNameFormatterTest {

    @Test
    public void testFormatSqsQueueName_HappyPath() {
        String expected = "prod-foo-bar";
        String actual = SqsQueueNameFormatter.formatSqsQueueName("foo.bar", "prod");
        Assert.assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatSqsQueueName_NullQueueName() {
        SqsQueueNameFormatter.formatSqsQueueName(null, "prod");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatSqsQueueName_BlankQueueName() {
        SqsQueueNameFormatter.formatSqsQueueName(" ", "prod");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatSqsQueueName_NullEnv() {
        SqsQueueNameFormatter.formatSqsQueueName("foo-bar ", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatSqsQueueName_BlankEnv() {
        SqsQueueNameFormatter.formatSqsQueueName("foo-bar", " ");
    }

    @Test
    public void testFormatSqsQueueName_LimitsTo80Characters() {
        //SQS only allows queue names to be 80 chars in length.
        String originalQueueName = StringUtils.rightPad("", 300, "a");
        String actual = SqsQueueNameFormatter.formatSqsQueueName(originalQueueName, "prod");
        String expected = StringUtils.rightPad("prod-", 80, "a");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFormatSqsQueueName_ReplacesNonAlphaNumeric() {
        String expected = "prod-foo-bar";
        String actual = SqsQueueNameFormatter.formatSqsQueueName("foo*bar", "prod");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFormatSqsQueueName_DoesNotTouchExistingDashes() {
        String expected = "prod-foo-bar";
        String actual = SqsQueueNameFormatter.formatSqsQueueName("foo-bar", "prod");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFormatSqsQueueName_DevEnvIncludesHost() {
        //No f'n clue what host will be or how to mock InetAddress.getLocalHost().getHostName(), so use regex
        String actual = SqsQueueNameFormatter.formatSqsQueueName("foo-bar", "dev");
        Assert.assertTrue("\"" +actual + "\" did not match regex of \"dev-[a-zA-Z0-9-_]+-foo-bar\"",
                actual.matches("dev-[a-zA-Z0-9-_]+-foo-bar"));
    }

    @Test
    public void testFormatSqsQueueName_EnvWithInvalidCharacter() {
        //No f'n clue what host will be or how to mock InetAddress.getLocalHost().getHostName(), so use regex
        String actual = SqsQueueNameFormatter.formatSqsQueueName("foo-bar", "dev^env");
        Assert.assertEquals("dev-env-foo-bar",actual);
    }
}
