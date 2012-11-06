package com.streamreduce.core.model.messages;

import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;

import org.bson.types.ObjectId;
import org.junit.Test;

import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;

public class SobaMessageTest {

    @Test
    public void testCreateSobaMessageRequiredFields() {
        //Happy Path for creating a SobaMessage from its Builder.
       new SobaMessage.Builder()
               .type(MessageType.INVENTORY_ITEM)
               .visibility(SobaObject.Visibility.ACCOUNT)
               .sender(createValidSender())  //sets ownerId and senderId
               .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateSobaMessageNoType() {
        //Verify that building a SobaMessage without a type fails
        new SobaMessage.Builder()
                .visibility(SobaObject.Visibility.ACCOUNT)
                .sender(createValidSender())  //sets ownerId and senderId
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateSobaMessageNullVisibility() {
        //Verify that building a SobaMessage with null visibility fails
        new SobaMessage.Builder()
                .type(MessageType.INVENTORY_ITEM)
                .sender(createValidSender())  //sets ownerId and senderId
                .visibility(null)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateSobaMessageNoSender() {
        //Verify that building a SobaMessage without specifying a sender fails
        new SobaMessage.Builder()
                .type(MessageType.INVENTORY_ITEM)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateSobaMessageSenderHasNoIds() {
        //Verify that building a SobaMessage with senders not having IDs fails.
        new SobaMessage.Builder()
                .type(MessageType.INVENTORY_ITEM)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .sender(createInvalidSenderWithNoIds())
                .build();
    }

    @Test
    public void testCreateSobaMessage_IncludesSenderAccountId() {
        SobaObject sender = createValidSender();

        SobaMessage sobaMessage = new SobaMessage.Builder()
                .type(MessageType.INVENTORY_ITEM)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .sender(sender)
                .build();

        assertEquals(sender.getAccount().getId(), sobaMessage.getSenderAccountId());

    }

    private Connection createValidSender() {
        Account account = new Account.Builder().name("ho").build();
        account.setId(new ObjectId());

        User user = new User.Builder().username("hey").account(account).build();
        user.setId(new ObjectId());

        Connection connection = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .url("http://foo.url1.com/rss")
                .credentials(new ConnectionCredentials("ident", "pass"))
                .alias("connection1")
                .user(user)
                .authType(AuthType.NONE)
                .build();
        connection.setId(new ObjectId());

        return connection;

    }

    private Connection createInvalidSenderWithNoIds() {
        User user = new User.Builder().username("hey").account(new Account.Builder().name("ho").build()).build();

        Connection connection = new Connection.Builder()
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .url("http://foo.url1.com/rss")
                .credentials(new ConnectionCredentials("ident", "pass"))
                .alias("connection1")
                .user(user)
                .authType(AuthType.NONE)
                .build();

        return connection;
    }


}
