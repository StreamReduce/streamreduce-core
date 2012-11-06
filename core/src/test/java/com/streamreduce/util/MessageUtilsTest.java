package com.streamreduce.util;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;

public class MessageUtilsTest extends TestCase {

    @Test
    public void testCleanEntry() throws Exception {
        Assert.assertEquals("HELLO", MessageUtils.cleanEntry("<strong>HELLO</strong>")); // Test HTML tag removal
        Assert.assertEquals("\"HELLO\"", MessageUtils.cleanEntry("&quot;HELLO&quot;")); // Test unescaping HTML entities
        Assert.assertEquals("Nodeable's", MessageUtils.cleanEntry("Nodeable&#39;s")); // Test unicode
    }
}
