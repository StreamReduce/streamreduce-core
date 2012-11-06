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
