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

package com.streamreduce.core.dao;

import com.google.code.morphia.AdvancedDatastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.Query;
import com.streamreduce.Constants;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageComment;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.util.ConnectionUtils;
import com.streamreduce.util.MessageUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository("sobaMessageDAO")
public class SobaMessageDAO extends ValidatingDAO<SobaMessage> {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected SobaMessageDAO(@Qualifier(value = "messageDBDatastore") AdvancedDatastore datastore) {
        super(datastore);
    }

    /*
     * Get all messages for an account
     */
    public List<SobaMessage> getMessagesFromInbox(User user, Long after, Long before, int limit, boolean ascending, String search, List<String> hashtags, String sender, boolean excludeNodebellies) {

        Query<SobaMessage> query = ((AdvancedDatastore) getDatastore()).find(MessageUtils.getInboxPath(user), entityClazz);
        query.and(
                query.or(
                        query.and(
                                // self and you are that person
                                query.criteria("ownerId").hasThisOne(user.getId()),
                                query.criteria("visibility").equal(SobaObject.Visibility.SELF)
                        )
                        ,
                        query.and(
                                // you always get these, these also include User sent messages as they are account scoped now
                                query.criteria("visibility").equal(SobaObject.Visibility.ACCOUNT)
                        )
                        ,
                        query.and(
                                // this is your copy of a public message
                                query.criteria("visibility").equal(SobaObject.Visibility.PUBLIC)
                        )));

        if (!StringUtils.isBlank(search)) {
            query.and(
                    query.or(
                            query.criteria("senderName").containsIgnoreCase(search),
                            query.criteria("transformedMessage").containsIgnoreCase(search),
                            query.criteria("comments").hasThisElement(new MessageComment(search))
                    )
            );
        }

        //OR filter over hashtag and sender fields
        List<Criteria> hashTagAndSenderCriteria = new ArrayList<Criteria>();
        if (hashtags != null && hashtags.contains("#conversation")) {
            hashTagAndSenderCriteria.add(query.criteria("hashtags").hasAnyOf(hashtags));
        } else if (hashtags != null && hashtags.size() > 0) {
            hashTagAndSenderCriteria.add(query.criteria("hashtags").hasAllOf(hashtags));
        }

        if (sender != null) {
            try {
                ObjectId senderId = new ObjectId(sender);
                hashTagAndSenderCriteria.add(
                        query.or(
                                query.criteria("senderId").equal(senderId),
                                query.criteria("senderName").equal(sender)
                        ));
            } catch (IllegalArgumentException e) { // only look at senderName if sender can't be parsed into an ObjectId
                hashTagAndSenderCriteria.add(query.criteria("senderName").equal(sender));
            }
        }
        if (!hashTagAndSenderCriteria.isEmpty()) {
            query.or(hashTagAndSenderCriteria.toArray(new Criteria[hashTagAndSenderCriteria.size()]));
        }

        if (after != null) {
            query.field("modified").greaterThan(after);
        }
        if (before != null) {
            query.field("modified").lessThan(before);
        }
        if (limit == 0) {
            limit = Constants.DEFAULT_MAX_MESSAGES;
        }
        if (limit > 0) {
            query.limit(limit);
        }
        if (excludeNodebellies) {
            query.criteria("type").notEqual(MessageType.NODEBELLY);
        }

        String order = "modified";
        if (!ascending) {
            order = "-" + order;
        }
        query.order(order);

        return query.asList();
    }

    public void saveToInbox(Account account, SobaMessage message) {
        logger.debug("[SOBA MESSAGE DAO] saving new message: " + message.getTransformedMessage());
        ((AdvancedDatastore) getDatastore()).save(MessageUtils.getMessageInboxPath(account), message);
    }

    public void saveToInboxes(List<Account> accounts, SobaMessage message) {
        for (Account account : accounts) {
            // save if it hasn't been blacklisted (this should be for stream and insight messages
            if (!ConnectionUtils.isBlacklisted(account, message.getConnectionId())) {
                saveToInbox(account, message);
            }
        }
    }

    // TODD: limit by date?
    public List<SobaMessage> getPublicArchivedMessages() {
        Query<SobaMessage> query = ds.find(entityClazz);
        query.criteria("visibility").equal(SobaObject.Visibility.PUBLIC);
        query.criteria("created").greaterThan(new Date().getTime() - Constants.BOOTSTRAP_MESSAGE_ARCHIVE_DURATION); // Now() - 1 week
        return query.asList();

    }

    public SobaMessage getFromInbox(Account account, ObjectId messageId) {
        return ((AdvancedDatastore) getDatastore()).get(MessageUtils.getMessageInboxPath(account), entityClazz, messageId);
    }

    // just for testing... i don't see a need for this really.
    // well, i guess we can always retract a sent messages with this method.
    public void deleteFromInbox(Account account, ObjectId messageId) {
        ((AdvancedDatastore) getDatastore()).delete(MessageUtils.getMessageInboxPath(account), messageId);
    }

    public void removeInbox(Account account) {
        getDatastore().delete(MessageUtils.getMessageInboxPath(account));
    }

    public void removeMessagesFromConnection(Account account, ObjectId connectionId) {
        // What is the syntax to do this with .delete()?????
        Query<SobaMessage> query = ((AdvancedDatastore) getDatastore()).find(MessageUtils.getMessageInboxPath(account), entityClazz);
        query.criteria("connectionId").equal(connectionId);
        List<SobaMessage> sobaMessages = query.asList();
        for (SobaMessage sobaMessage : sobaMessages) {
            deleteFromInbox(account, sobaMessage.getId());
        }
    }
}
