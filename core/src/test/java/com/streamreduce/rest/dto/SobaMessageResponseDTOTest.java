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

package com.streamreduce.rest.dto;

import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

public class SobaMessageResponseDTOTest {

    @Test
    public void testIncludesSenderConnectionId() {
        SobaObject sender = createValidSender();

        SobaMessage sobaMessage = new SobaMessage.Builder()
                .type(MessageType.INVENTORY_ITEM)
                .visibility(SobaObject.Visibility.ACCOUNT)
                .sender(sender)
                .build();
        SobaMessageResponseDTO responseDTO = SobaMessageResponseDTO.fromSobaMessage(sobaMessage);
        Assert.assertEquals(sender.getAccount().getId(),responseDTO.getSenderAccountId());
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


}
