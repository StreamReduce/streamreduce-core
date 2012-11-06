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

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class HashtagUtilTest {

    @Test
    public void testNormalizeHashtag() {
        //Tests expected behavior of HashtagUtil.normalizeTag
        String normalized = HashtagUtil.normalizeTag("Foo");
        assertEquals(normalized,"#foo");
    }

    @Test
    public void testNormalizeHashtagTrimWhitespace() {
        //ensures normalizeTag trims whitespace
        String normalized = HashtagUtil.normalizeTag("  Foo   ");
        assertEquals(normalized,"#foo");
    }

    @Test
    public void testNormalizeHashtagOnlyWhitespace() {
        //ensures normalizeTag returns the same empty String
        String orig = "   ";
        String normalized = HashtagUtil.normalizeTag(orig);
        assertEquals(normalized,orig);
    }

    @Test
    public void testNormalizeHashtagNull() {
        //ensures normalizeTag returns null if passed null
        String orig = null;
        String normalized = HashtagUtil.normalizeTag(orig);
        assertEquals(normalized,orig);
    }

    @Test
    public void testNormalizeTags() {
        List<String> origTags = Lists.newArrayList("Foo","  Bar");
        List<String> normalized = HashtagUtil.normalizeTags(origTags);
        assertEquals(Lists.newArrayList("#foo","#bar"),normalized);
    }

    @Test
    public void testNormalizeTagsNoNullsOrBlanks() {
        List<String> origTags = Lists.newArrayList("#Foo","  Bar",null,"   ");
        List<String> normalized = HashtagUtil.normalizeTags(origTags);
        assertEquals(Lists.newArrayList("#foo","#bar"),normalized);
    }

    @Test
    public void testNormalizeTagsNullInput() {
        List<String> origTags = null;
        List<String> normalized = HashtagUtil.normalizeTags(origTags);
        assertEquals(Collections.emptyList(),normalized);
    }
}
