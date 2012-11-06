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

package com.streamreduce.core.service;

import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.Event;
import com.streamreduce.core.model.InventoryItem;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageComment;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.core.model.messages.details.SobaMessageDetails;
import com.streamreduce.core.service.exception.MessageNotFoundException;
import com.streamreduce.core.service.exception.TargetNotFoundException;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bson.types.ObjectId;

public interface MessageService {

    // a stream of *all* messages
    List<SobaMessage> getAllMessages(User currentUser, Long after, Long before, int max, boolean ascending, String search, List<String> hashTags, String sender, boolean excludeNodebellies);

    // we figure out MessageScope (storage location) server side
    SobaMessage getMessage(Account account, ObjectId messageId) throws MessageNotFoundException;

    // helper messages for public messages, that should be dropped in all inboxes
    // reserved for Nodebelly user only
    @SuppressWarnings("rawtypes")
    void sendNodebellyGlobalMessage(Event event, SobaObject sender, Connection connection, Long dateGenerated,
                                    MessageType type, Set<String> hashtags);

    // reserved for Nodeable System User only
    @SuppressWarnings("rawtypes")
    SobaMessage sendNodeableAccountMessage(Event event, Account account, Set<String> hashtags);

    @SuppressWarnings("rawtypes")
    SobaMessage sendConnectionMessage(Event event, Connection connection);

    @SuppressWarnings("rawtypes")
    SobaMessage sendInventoryMessage(Event event, InventoryItem inventoryItem);

    SobaMessage sendActivityMessage(Event event, Connection connection, Long dateGenerated, SobaMessageDetails details);

    SobaMessage sendGatewayMessage(Event event, Connection connection, Long dateGenerated);

    SobaMessage sendGatewayMessage(Event event, InventoryItem inventoryItem, Long dateGenerated);

    @SuppressWarnings("rawtypes")
    SobaMessage sendAccountMessage(Event event, SobaObject sender, Connection connection, Long dateGenerated,
                                   MessageType type, Set<String> hashtags, SobaMessageDetails details);

    @SuppressWarnings("rawtypes")
    SobaMessage sendNodebellyInsightMessage(Event event, Long dateGenerated, Set<String> hashtags);

    // just for user to user right now.
    SobaMessage sendUserMessage(Event event, User sender, String message) throws TargetNotFoundException;

    void updateMessage(Account account, SobaMessage sobaMessage) throws MessageNotFoundException;

    void addCommentToMessage(Account account, ObjectId messageId, MessageComment comment, @Nullable Set<String> hashtags) throws MessageNotFoundException;

    void addHashtagToMessage(Account account, ObjectId messageId, String hashtag) throws MessageNotFoundException;

    void removeHashtagFromMessage(Account account, ObjectId messageId, String hashtag) throws MessageNotFoundException;

    // boostrap new accounts with public and global messages
    void copyArchivedMessagesToInbox(Account account);

    void removeSampleMessages(Account account, ObjectId connectionId);

    void removeAllMessages(Account account);

    void nullifyMessage(User user, SobaMessage sobaMessage);

    void nullifyMessageComment(User user, SobaMessage sobaMessage, MessageComment messageComment);
}
