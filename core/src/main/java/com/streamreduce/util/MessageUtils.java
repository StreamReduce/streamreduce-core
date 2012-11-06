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

import com.streamreduce.Constants;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.SobaMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessageUtils {

    public static ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();
    public static Logger LOGGER = LoggerFactory.getLogger(MessageUtils.class);


    /* Reads in a JSON file from the classpath.
    *
    * @param resource the resource to read in as a string
    *
    * @return the contents of the resource as string
    *
    * @throws Exception if anything goes wrong
    */
    public static String readJSONFromClasspath(String resource) {
        InputStream inputStream = MessageUtils.class.getResourceAsStream(resource);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(inputStream, writer, "UTF-8");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return writer.toString();
    }


    public static String getInboxPath(User user) {
        return getMessageInboxPath(user.getAccount());
    }

    public static String getMessageInboxPath(Account account) {
        return Constants.INBOX_COLLECTION_PREFIX + account.getFuid();
    }

    public static String getMetricInboxPath(Account account) {
        return Constants.METRIC_COLLECTION_PREFIX + account.getId();
    }

    public static String roundAndTruncate(double rawValue, int precision) {
        DecimalFormat df = new DecimalFormat();

        df.setMaximumFractionDigits(precision);
        df.setMinimumFractionDigits(precision);
        df.setMinimumIntegerDigits(1);

        return df.format(rawValue);
    }

    // TODO: Refactor this to be a more standard converter.  (Might be able to use something already available.)
    public static double kbToGB(double kb) {
        return kb / (1024 * 1024);
    }

    public class ParsedMessage {
        String message;
        Set<String> target;
        Set<String> tags;
        boolean justTags = true;

        public ParsedMessage() {
            target = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            tags = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        }

        public Set<String> getTarget() {
            return target;
        }

        public Set<String> getTags() {
            return tags;
        }

        public void addTarget(String s) {
            target.add(s);
        }

        public void addTag(String s) {
            tags.add(s);
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isJustTags() {
            return justTags;
        }

        public void setJustTags(boolean justTags) {
            this.justTags = justTags;
        }

    }

    /**
     * Returns a {@link ParsedMessage} object with username and hashtag lists
     * from the given string. Also includes the message with normalized (lowercase) tags if it had any
     *
     * @param s the message to extract usernames and hashtags from.
     * @return the {@link ParsedMessage} object with lists of Strings.
     */
    public static ParsedMessage parseMessage(String s) {
        //ParsedMessage pm = MessageUtils.new ParsedMessage(); // TODO why can't i do this?
        MessageUtils mu = new MessageUtils();
        ParsedMessage pm = mu.new ParsedMessage();
        s = s.replaceAll("\t", " ");
        String[] words = s.split(" ");

        for (String word : words) {
            word = word.trim();
            if (word.length() > 0) {
                if (word.charAt(0) == '@') {
                    // remove punctuation if it exists
                    pm.addTarget(word.substring(1).replaceAll("\\W", ""));
                } else if (word.charAt(0) == '#') {
                    // replace word in the original string
                    s = s.replaceAll(word, word.toLowerCase());
                    pm.addTag(word);
                } else {
                    // is just regular text
                    pm.setJustTags(false);
                }
            }
        }
        pm.setMessage(s);
        return pm;
    }

    public static String cleanEntry(String entry) {
        if (entry == null) {
            return null;
        }
        return StringEscapeUtils.unescapeHtml(entry.replaceAll("\\<.*?>", "")
                .replaceAll("\\s", " ")
                .replaceAll(" +", " ")
                .trim());
    }

    public static String hashMessage(SobaMessage sobaMessage) {
        String hashValue = sobaMessage.getTransformedMessage() + sobaMessage.getSenderName();
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            // should never happen
        }
        return hexEncode(sha.digest(hashValue.getBytes()));
    }

    public static String hexEncode(byte[] aInput) {
        StringBuilder result = new StringBuilder();
        char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        for (byte b : aInput) {
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }
}
