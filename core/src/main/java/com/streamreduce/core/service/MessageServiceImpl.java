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

import com.streamreduce.ConnectionNotFoundException;
import com.streamreduce.OutboundStorageException;
import com.streamreduce.connections.ConnectionProvider;
import com.streamreduce.connections.ConnectionProviderFactory;
import com.streamreduce.core.dao.SobaMessageDAO;
import com.streamreduce.core.event.EventId;
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
import com.streamreduce.core.service.exception.AccountNotFoundException;
import com.streamreduce.core.service.exception.MessageNotFoundException;
import com.streamreduce.core.service.exception.TargetNotFoundException;
import com.streamreduce.core.transformer.MessageTransformerResult;
import com.streamreduce.core.transformer.SobaMessageTransformerFactory;
import com.streamreduce.util.HashtagUtil;
import com.streamreduce.util.MessageUtils;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("messageService")
public class MessageServiceImpl extends AbstractService implements MessageService {

    @Autowired
    private SobaMessageDAO sobaMessageDAO;
    @Autowired
    private ConnectionProviderFactory connectionProviderFactory;
    @Autowired
    private OutboundStorageService outboundStorageService;
    @Autowired
    private UserService userService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private EventService eventService;
    @Autowired
    private EmailService emailService;

    @Resource(name = "messageProperties")
    public Properties messageProperties;

    @Override
    public List<SobaMessage> getAllMessages(User currentUser, Long after, Long before, int limit, boolean ascending, String search, List<String> hashTags, String sender, boolean excludeNodebellies) {
        return sobaMessageDAO.getMessagesFromInbox(currentUser, after, before, limit, ascending, search, HashtagUtil.normalizeTags(hashTags), sender, excludeNodebellies);
    }

    @Override
    public SobaMessage getMessage(Account account, ObjectId messageId) throws MessageNotFoundException {
        SobaMessage message = sobaMessageDAO.getFromInbox(account, messageId);
        if (message == null) {
            throw new MessageNotFoundException(messageId.toString());
        }
        return message;
    }

    // for all inboxes, will create mult
    @SuppressWarnings("rawtypes")
    @Override
    public void sendNodebellyGlobalMessage(Event event, SobaObject sender, Connection connection,
                                           Long dateGenerated, MessageType type, Set<String> hashtags) {
        this.createMessage(event, sender, connection, dateGenerated, type, hashtags, null);
    }

    // for a certain account
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public SobaMessage sendNodeableAccountMessage(Event event, Account account, Set<String> hashtags) {

        User sender = userService.getSystemUser();
        // this enables us to save in a target account rather than use the senders account
        sender.setAccount(account);
        return sendAccountMessage(
                event,
                sender, // sent from nodebelly
                null, // no connectionId right now
                new Date().getTime(),
                MessageType.SYSTEM,
                hashtags,
                null
        );
    }

    @Override
    public SobaMessage sendConnectionMessage(Event event, Connection connection) {
        return sendAccountMessage(
                event,
                connection,
                connection, // happens to be the same as sender
                new Date().getTime(),
                MessageType.CONNECTION,
                connection.getHashtags(),
                null
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public SobaMessage sendInventoryMessage(Event event, InventoryItem inventoryItem) {
        return sendAccountMessage(event,
                inventoryItem,
                inventoryItem.getConnection(),
                new Date().getTime(),
                MessageType.INVENTORY_ITEM,
                inventoryItem.getHashtags(),
                null
        );
    }

    @Override
    public SobaMessage sendActivityMessage(Event event, Connection connection, Long dateGenerated, SobaMessageDetails details) {
        return sendAccountMessage(
                event,
                connection,
                connection, // happens to be the same as sender
                dateGenerated,
                MessageType.ACTIVITY,
                connection.getHashtags(),
                details
        );
    }

    @Override
    public SobaMessage sendGatewayMessage(Event event, Connection connection, Long dateGenerated) {
        return sendAccountMessage(
                event,
                connection,
                connection, // happens to be the same as sender
                dateGenerated,
                MessageType.GATEWAY,
                connection.getHashtags(),
                null
        );
    }

    @Override
    public SobaMessage sendGatewayMessage(Event event, InventoryItem inventoryItem, Long dateGenerated) {
        return sendAccountMessage(
                event,
                inventoryItem,
                inventoryItem.getConnection(),
                dateGenerated,
                MessageType.GATEWAY,
                inventoryItem.getHashtags(),
                null
        );
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SobaMessage sendAccountMessage(Event event, SobaObject sender, Connection connection,
                                          Long dateGenerated, MessageType type, Set<String> hashtags, SobaMessageDetails details) {
        return createMessage(event, sender, connection, dateGenerated, type, hashtags, details);
    }

    @Override
    public SobaMessage sendNodebellyInsightMessage(Event event, Long dateGenerated, Set<String> hashtags) {
        if (event == null || dateGenerated == null) {
            throw new IllegalArgumentException("event and dateGenerated must be non-null");
        }
        hashtags = hashtags == null ? new HashSet<String>() : hashtags;

        SobaMessage sobaMessage = null;

        try {
            // if this is the sender, should we override the ownerId? how?
            User nodebelly = userService.getSuperUser();

            // if this is null, it's a global metric so only persist the message to the Nodeable account
            if (event.getAccountId() != null) {
                Account account = userService.getAccount(event.getAccountId());

                // send an email if these are your first insights
                // SOBA-1600
                if (!account.getConfigValue(Account.ConfigKey.RECIEVED_INSIGHTS)) {
                    userService.handleInitialInsightForAccount(account);
                }

                // trickery to send from Nodebelly to a certain account
                // DO NOT PERSIST THIS!!!!!
                // also note how this is set last so the account references above are still valid to the "real" account
                nodebelly.setAccount(account);
            }

            Map<String, Object> meta = event.getMetadata();
            String objectType = (String) meta.get("targetType");
            String providerTypeId = (String) meta.get("targetProviderId");

            // filter out inventory count messages, since they are redundant with connection counts
            String metricName = (String) meta.get("name");
            if (metricName.startsWith("INVENTORY_ITEM_COUNT.")) {
                return null;
            }

            // get the connection where applicable
            Connection connection = null;
            try {
                ObjectId connectionId = null;
                // ugh
                if (objectType.contains("Connection")) {
                    connectionId = event.getTargetId();
                } else if (objectType.contains("InventoryItem")) {
                    connectionId = new ObjectId(meta.get("targetConnectionId").toString());
                }
                connection = connectionService.getConnection(connectionId);
            } catch (ConnectionNotFoundException e) {
                logger.error("Invalid connection Id, can not send Nodebelly Message" + e.getMessage());
            }

            // custom tags
            hashtags.add("#insight");
            if (providerTypeId != null) {
                hashtags.add(HashtagUtil.normalizeTag(providerTypeId)); // ie, aws, github, jira (this should be what we currently aggregate on
            }
            hashtags.add(HashtagUtil.toNodebellyTag(event.getEventId())); // based suffix of eventId

            sobaMessage = this.createMessage(event, nodebelly, connection, dateGenerated, MessageType.NODEBELLY, hashtags, null);
        } catch (AccountNotFoundException e) {
            logger.error(e.getMessage());
        }
        return sobaMessage;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SobaMessage sendUserMessage(Event event, User sender, String message) throws TargetNotFoundException {
        // Not sure if this is possible but since the EventService can return null for created events and we want to
        // avoid NPEs, let's be safe.
        if (event == null) {
            return null;
        }

        MessageUtils.ParsedMessage parsedMessage = MessageUtils.parseMessage(message);
        Set<String> hashtags = parsedMessage.getTags();
        // this is now a conversation (even if with no one), so add that tag
        hashtags.add("#conversation");

        SobaMessage sobaMessage = createMessage(event,
                sender,
                null,
                new Date().getTime(),
                MessageType.USER,
                hashtags,
                null);
        emailService.sendUserMessageAddedEmail(sender, sobaMessage);
        return sobaMessage;
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    protected SobaMessage createMessage(Event event, SobaObject sender,
                                        Connection connection, Long dateGenerated,
                                        MessageType type, Set<String> hashtags, @Nullable SobaMessageDetails details) {
        // Anytime the event is null, we do not want to send the requested message
        if (event == null) {
            return null;
        }

        // convert to a more user friendly string
        // accept messageDetails (if any) so we can append info if needed
        MessageTransformerResult messageTransformerResult =
                SobaMessageTransformerFactory.transformMessage(event, type, details, messageProperties);

        // plain text
        String transformedMessage = messageTransformerResult.getTransformedMessage();
        // embed the rich formatted message in the soba object
        SobaMessageDetails sobaMessageDetails = messageTransformerResult.getMessageDetails();

        SobaMessage message = new SobaMessage.Builder()
                .sender(sender)
                .dateGenerated(dateGenerated)
                .type(type)
                .connection(connection)
                .transformedMessage(transformedMessage)
                .hashtags(hashtags)
                .details(sobaMessageDetails)
                .build();

        // save to inbox(es)
        saveMessage(sender, message);

        // Fire the event that a message has been created
        Map<String, Object> eventMetadata = new HashMap<>();

        // Message event metadata
        eventMetadata.put("messageEventId", event.getId());
        eventMetadata.put("messageEventAccountId", event.getAccountId());
        eventMetadata.put("messageEventTargetId", event.getTargetId());
        eventMetadata.put("messageEventTargetType", event.getMetadata().get("targetType"));
        eventMetadata.put("messageEventUserId", event.getUserId());
        eventMetadata.put("messageEventEventId", event.getEventId());

        // General metadata
        eventMetadata.put("messageDateGenerated", dateGenerated);
        eventMetadata.put("messageType", type);
        eventMetadata.put("messageHashtags", hashtags);
        eventMetadata.put("messagePlainContent", transformedMessage);

        // Sender metadata
        eventMetadata.put("messageSenderId", sender.getId());
        eventMetadata.put("messageSenderType", sender.getClass().getSimpleName());
        eventMetadata.put("messageSenderUserId", sender.getUser() != null ? sender.getUser().getId() : null);
        eventMetadata.put("messageSenderAccountId", sender.getAccount() != null ? sender.getAccount().getId() : null);
        eventMetadata.put("messageSenderAlias", sender.getAlias());
        eventMetadata.put("messageSenderHashtags", sender.getHashtags());
        eventMetadata.put("messageSenderVisibility", sender.getVisibility());
        eventMetadata.put("messageSenderVersion", sender.getVersion());

        // Connection metadata
        eventMetadata.put("messageConnectionId", connection != null ? connection.getId() : null);
        eventMetadata.put("messageConnectionAlias", connection != null ? connection.getAlias() : null);
        eventMetadata.put("messageConnectionHashtags", connection != null ? connection.getHashtags() : null);
        eventMetadata.put("messageConnectionVersion", connection != null ? connection.getVersion() : null);

        // Connection provider metadata
        ConnectionProvider cProvider = connection != null ?
                connectionProviderFactory.connectionProviderFromId(connection.getProviderId()) :
                null;

        eventMetadata.put("messageProviderId", cProvider != null ? cProvider.getId() : null);
        eventMetadata.put("messageProviderDisplayName", cProvider != null ? cProvider.getDisplayName() : null);
        eventMetadata.put("messageProviderType", cProvider != null ? cProvider.getType() : null);

        eventService.createEvent(EventId.CREATE, message, eventMetadata);

        // the the message and creation event, attempt to persist to outbound.
        try {
            outboundStorageService.sendSobaMessage(message);
        } catch (OutboundStorageException e) {
            logger.error("Unable to save processed message to outbound connections", e);
        }

        return message;
    }


    @SuppressWarnings("rawtypes")
    protected void saveMessage(SobaObject sender, SobaMessage message) {
        SobaObject.Visibility visibility = message.getVisibility();
        switch (visibility) {
            case PUBLIC:
                // give a copy to everyone
                sobaMessageDAO.saveToInboxes(userService.getAccounts(), message);
                // save in public archive repo
                sobaMessageDAO.save(message);
                break;

            default:
                // all other types are just in the one account inbox
                sobaMessageDAO.saveToInbox(sender.getAccount(), message);
                break;
        }
    }


    @Override
    public void updateMessage(Account account, SobaMessage sobaMessage) throws MessageNotFoundException {
        // verify it exists first
        SobaMessage message = sobaMessageDAO.getFromInbox(account, sobaMessage.getId());
        if (message == null) {
            throw new MessageNotFoundException(sobaMessage.getId().toString());
        }
        sobaMessageDAO.saveToInbox(account, sobaMessage);
    }


    @Override
    public void addCommentToMessage(Account account, ObjectId messageId, MessageComment comment, Set<String> hashtags)
            throws MessageNotFoundException {
        SobaMessage message = getMessage(account, messageId);
        message.addComment(comment);
        if (hashtags != null && !hashtags.isEmpty()) {
            message.addHashtags(hashtags);
        }
        updateMessage(account, message);
        emailService.sendCommentAddedEmail(account, message, comment);
    }

    @Override
    public void addHashtagToMessage(Account account, ObjectId messageId, String hashtag) throws MessageNotFoundException {
        SobaMessage message = getMessage(account, messageId);

        message.addHashtag(hashtag);
        updateMessage(account, message);
    }

    @Override
    public void removeHashtagFromMessage(Account account, ObjectId messageId, String hashtag) throws MessageNotFoundException {
        SobaMessage message = getMessage(account, messageId);
        message.removeHashtag(hashtag);
        updateMessage(account, message);
    }

    @Override
    public void copyArchivedMessagesToInbox(Account account) {
        // get all messages from the archive.
        List<SobaMessage> messages = sobaMessageDAO.getPublicArchivedMessages();
        logger.info("Copying " + messages.size() + " archived messages to account inbox " + account.getFuid());
        for (SobaMessage message : messages) {
            sobaMessageDAO.saveToInbox(account, message);
        }
    }

    @Override
    public void removeSampleMessages(Account account, ObjectId connectionId) {
        sobaMessageDAO.removeMessagesFromConnection(account, connectionId);
    }

    /**
     * Remove all messages and the collection for this account.
     *
     * @param account - the account to kill
     */
    @Override
    public void removeAllMessages(Account account) {
        sobaMessageDAO.removeInbox(account);
    }

    @Override
    public void nullifyMessage(User user, SobaMessage sobaMessage) {
        try {
            String nullify = MessageFormat.format(messageProperties.getProperty("message.removed.by"),
                    user.getAlias());
            sobaMessage.setDetails(null);
            sobaMessage.setTransformedMessage(nullify);
            updateMessage(user.getAccount(), sobaMessage);
        } catch (MessageNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void nullifyMessageComment(User user, SobaMessage sobaMessage, MessageComment messageComment) {
        try {
            String nullify = MessageFormat.format(messageProperties.getProperty("message.comment.removed.by"),
                    user.getAlias());
            for (MessageComment comment : sobaMessage.getComments()) {
                if (comment.equals(messageComment)) {
                    comment.setComment(nullify);
                    comment.setCreated(new Date().getTime());
                    updateMessage(user.getAccount(), sobaMessage);
                    break;
                }
            }

        } catch (MessageNotFoundException e) {
            logger.error(e.getMessage(), e);
        }


    }
}
