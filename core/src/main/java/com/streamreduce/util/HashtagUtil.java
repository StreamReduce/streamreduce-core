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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.streamreduce.core.event.EventId;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

public final class HashtagUtil {

    /**
     * Normalizes a string meant to be used as hashtag. Ensures that the returned string is suitable for use as a
     * hashtag by transforming the passed in string to
     * <ul>
     * <li>trim all leading/trailing whitespace</li>
     * <li>including a # as the first character</li>
     * <li>making all character in the String lower case</li>
     * </ul>
     *
     * @param text The string to be normalized in to a hashtag
     * @return the hashtag normalized String, or a null or empty string if either null or "" are passed in
     */
    public static String normalizeTag(String text) {
        if (!StringUtils.isBlank(text)) {
            text = text.trim();
            // if there is no # symbol add it.
            if (!(text.charAt(0) == '#')) {
                text = "#" + text;
            }
            text = text.toLowerCase();
        }
        return text;
    }

    /**
     * Normalizes a Collection of potential hashtags by filtering out all
     * empty/null Strings in the collection and then calls normalizeTag(String) on each remaining String.
     *
     * @param tags a collection of potential tags to normalize.
     * @return a List of normalized Strings from the passed in list, or a modifiable empty List if tags was null.
     */
    public static List<String> normalizeTags(Collection<String> tags) {
        if (tags == null) {
            return Lists.newArrayList();
        }

        //First filter out null and empty strings
        return Lists.newArrayList(Iterables.transform(Iterables.filter(tags, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String input) {
                return !StringUtils.isBlank(input);
            }
        }), new Function<String, String>() {
            @Override
            public String apply(@Nullable String input) {
                return normalizeTag(input);
            }
        }));
    }


    public static String getStringFromTag(String tag) {
        if (isValidTag(tag)) {
            tag = tag.substring(1, tag.length());
            // is functional
            if (tag.endsWith("#")) {
                tag = tag.substring(0, tag.length() - 1);
            }
            return tag;
        }
        return tag; // avoid NPE
    }



    public static boolean isValidTag(String tag) {
        return tag != null && !tag.isEmpty() && (tag.trim().charAt(0) == '#');
    }


    /**
     * Convert EventId.toString() NODEBELLY_FOO EventIds to tags. The tag is whatever is after the underscore character.
     * So if the EventId is NODEBELLY_FOO, the string returned is #foo -- note the tag is normalized
     *
     * @param eventId - an eventId that starts with Nodebelly
     * @return - normalized tag
     */
    public static String toNodebellyTag(String eventId) {
        if (eventId.startsWith("NODEBELLY_")) {
            int pos = eventId.indexOf("_") + 1;
            return normalizeTag(eventId.substring(pos));
        }
        return null;
    }

    public static String toNodebellyTag(EventId eventId) {
        return toNodebellyTag(eventId.toString());
    }


    public static Set<String> getCriticalTags() {
        Set<String> critical = new HashSet<String>();
        critical.add("#critical");
        return critical;
    }

}
