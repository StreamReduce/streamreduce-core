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

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.google.common.collect.Lists;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageComment;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.util.JSONObjectBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import junit.framework.Assert;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 7/5/12 7:51 PM</p>
 */
public class EmailServiceImplTest {

    public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi euismod, " +
            "lectus a posuere sollicitudin, ligula risus vestibulum purus, vel tincidunt nisi sem vel felis. " +
            "Suspendisse velit elit, pharetra id semper id, ullamcorper sed leo. Fusce gravida vestibulum fermentum. " +
            "Morbi commodo, quam nec vulputate condimentum, urna nunc ultricies purus, et ullamcorper nisi arcu ut " +
            "mauris. Praesent eleifend imperdiet interdum. Suspendisse potenti. Duis viverra, ipsum nec mattis cursus, " +
            "lorem ante hendrerit massa, iaculis varius leo mi sed mauris.";

    private Account testAccount = new Account.Builder()
            .url("http://nodeable.com")
            .description("Nodeable Test Account")
            .name("Nodeable Testing")
            .build();
    private User testUser = new User.Builder()
            .account(testAccount)
            .accountLocked(false)
            .accountOriginator(true)
            .fullname("Nodeable Test User")
            .username("test_user_" + new Date().getTime() + "@nodeable.com")
            .secretKey("s3Kret")
            .build();
    private User testSenderUser = new User.Builder()
            .account(testAccount)
            .accountLocked(false)
            .accountOriginator(true)
            .fullname("Nodeable Test Sender User")
            .username("test_user_" + new Date().getTime() + "@nodeable.com")
            .secretKey("s3Kret")
            .build();

    @InjectMocks
    private EmailServiceImpl emailService = new EmailServiceImpl();

    @Mock
    private Properties mockEmailProperties;
    @Mock
    private AmazonSimpleEmailServiceClient mockAmazonSimpleEmailServiceClient;
    @Mock
    private UserService mockUserService;

    @Before
    public void init() throws Exception {
        testAccount.setId(new ObjectId());

        testUser.setConfigValue(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS, true);
        testUser.setConfigValue(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS, true);

        testUser.setId(new ObjectId());

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(emailService, "simpleEmailServiceClient", mockAmazonSimpleEmailServiceClient);
        ReflectionTestUtils.setField(emailService, "emailProperties", mockEmailProperties);
        ReflectionTestUtils.setField(emailService, "userService", mockUserService);
        ReflectionTestUtils.setField(emailService, "backgroundSend", false);
        when(mockEmailProperties.get(anyString())).thenReturn("TestTemplateToken");
        when(mockAmazonSimpleEmailServiceClient.sendEmail(any(SendEmailRequest.class))).thenReturn(new SendEmailResult().withMessageId("foo"));
        emailService.setEnabled(true);
        emailService.afterPropertiesSet();
    }

    @After
    public void afterTest() throws Exception {
        testUser.setConfigValue(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS, true);
        testUser.setConfigValue(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS, true);
    }

    @Test
    public void testSendInsightsAvailableEmail() throws Exception {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        emailService.sendInsightsAvailableEmail(Arrays.asList(testUser));
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        // WEEEEEEEEEEEEEEEEEEEEEEEEEEEE!
        Body body = captor.getValue().getMessage().getBody();
        Assert.assertFalse("Encountered unexpanded template token.", body.getHtml().getData().contains("${"));
        Assert.assertFalse("Encountered unexpanded template token.", body.getText().getData().contains("${"));
    }

    @Test
    public void testSendInviteUserActivationEmail() throws Exception {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        emailService.sendInviteUserActivationEmail(testUser);
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        Body body = captor.getValue().getMessage().getBody();
        Assert.assertFalse("Encountered unexpanded template token.", body.getHtml().getData().contains("${"));
        Assert.assertFalse("Encountered unexpanded template token.", body.getText().getData().contains("${"));
    }

    @Test
    public void testSendNewUserActivationEmail() throws Exception {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        emailService.sendNewUserActivationEmail(testUser);
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        Body body = captor.getValue().getMessage().getBody();
        Assert.assertFalse("Encountered unexpanded template token.", body.getHtml().getData().contains("${"));
        Assert.assertFalse("Encountered unexpanded template token.", body.getText().getData().contains("${"));
    }

    @Test
    public void testSendPasswordResetEmail() throws Exception {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        emailService.sendPasswordResetEmail(testUser, true);
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        Body body = captor.getValue().getMessage().getBody();
        Assert.assertFalse("Encountered unexpanded template token.", body.getHtml().getData().contains("${"));
        Assert.assertFalse("Encountered unexpanded template token.", body.getText().getData().contains("${"));
    }

    @Test
    public void testSendUserAccountSetupCompleteEmail() throws Exception {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        emailService.sendUserAccountSetupCompleteEmail(testUser);
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        Body body = captor.getValue().getMessage().getBody();
        Assert.assertFalse("Encountered unexpanded template token.", body.getHtml().getData().contains("${"));
        Assert.assertFalse("Encountered unexpanded template token.", body.getText().getData().contains("${"));
    }

    @Test
    public void testSendBugReport() throws Exception {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        emailService.sendBugReport("username", "company", "summary", "details", "debuginfo");
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        Body body = captor.getValue().getMessage().getBody();
        Assert.assertFalse("Encountered unexpanded template token.", body.getHtml().getData().contains("${"));
        Assert.assertFalse("Encountered unexpanded template token.", body.getText().getData().contains("${"));
    }


    @Test
    public void testSendCommentAddedEmail() throws Exception {
        SobaMessage sobaMessage = new SobaMessage.Builder()
                .sender(testUser)
                .providerId("test")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .transformedMessage(LOREM_IPSUM)
                .type(MessageType.USER)
                .build();
        sobaMessage.setId(new ObjectId());
        MessageComment messageComment = new MessageComment(testSenderUser, LOREM_IPSUM);

        when(mockUserService.allEnabledUsersForAccount(testAccount)).thenReturn(Lists.newArrayList(testUser));

        testSenderUser.setId(new ObjectId());

        emailService.sendCommentAddedEmail(testAccount, sobaMessage, messageComment);
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        String emailContent = captor.getValue().getMessage().getBody().getHtml().getData();
        Assert.assertFalse("Encountered unexpanded template token.", emailContent.contains("${"));
    }

    @Test
    public void testSendCommentAddedEmail_userConfigDisabled() throws Exception {
        SobaMessage sobaMessage = new SobaMessage.Builder()
                .sender(testUser)
                .providerId("test")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .transformedMessage(LOREM_IPSUM)
                .type(MessageType.USER)
                .build();
        sobaMessage.setId(new ObjectId());
        MessageComment messageComment = new MessageComment(testSenderUser, LOREM_IPSUM);

        when(mockUserService.allEnabledUsersForAccount(testAccount)).thenReturn(Lists.newArrayList(testUser));

        testUser.getConfig().put(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS, false);

        testSenderUser.setId(new ObjectId());

        emailService.sendCommentAddedEmail(testAccount, sobaMessage, messageComment);
        verify(mockAmazonSimpleEmailServiceClient, never()).sendEmail(null);
    }

    @Test
    public void testSendUserMessageEmail() throws Exception {
        SobaMessage sobaMessage = new SobaMessage.Builder()
                .sender(testUser)
                .providerId("test")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .transformedMessage(LOREM_IPSUM)
                .type(MessageType.USER)
                .build();
        sobaMessage.setId(new ObjectId());

        testUser.getConfig().put(User.ConfigKeys.RECEIVES_COMMENT_NOTIFICATIONS, false);

        testSenderUser.setId(new ObjectId());
        JSONObject payload = new JSONObjectBuilder()
                .add("subject", "Test Subject")
                .add("body", "Test Body")
                .add("recipient", "testuser@nodeable.com")
                .build();

        emailService.sendUserMessageEmail(testSenderUser, sobaMessage, payload);
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        String emailContent = captor.getValue().getMessage().getBody().getHtml().getData();
        Assert.assertFalse("Encountered unexpanded template token in email content: " + emailContent, emailContent.contains("${"));
    }

    @Test
    public void testSendUserMessageAddedEmail() throws Exception {
        SobaMessage sobaMessage = new SobaMessage.Builder()
                .sender(testUser)
                .providerId("test")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .transformedMessage(LOREM_IPSUM)
                .type(MessageType.USER)
                .build();
        sobaMessage.setId(new ObjectId());

        when(mockUserService.allEnabledUsersForAccount(testAccount)).thenReturn(Lists.newArrayList(testUser));

        testSenderUser.setId(new ObjectId());
        emailService.sendUserMessageAddedEmail(testSenderUser, sobaMessage);
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        String emailContent = captor.getValue().getMessage().getBody().getHtml().getData();
        Assert.assertFalse("Encountered unexpanded template token.", emailContent.contains("${"));
    }

    @Test
    public void testSendUserMessageAddedEmail_userConfigDisabled() throws Exception {
        SobaMessage sobaMessage = new SobaMessage.Builder()
                .sender(testUser)
                .providerId("test")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .transformedMessage(LOREM_IPSUM)
                .type(MessageType.USER)
                .build();
        sobaMessage.setId(new ObjectId());

        testUser.getConfig().put(User.ConfigKeys.RECEIVES_NEW_MESSAGE_NOTIFICATIONS, false);

        when(mockUserService.allEnabledUsersForAccount(testAccount)).thenReturn(Lists.newArrayList(testUser));

        testSenderUser.setId(new ObjectId());
        emailService.sendUserMessageAddedEmail(testSenderUser, sobaMessage);
        //ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        Mockito.verify(mockAmazonSimpleEmailServiceClient, never()).sendEmail(null);
    }

    @Test
    public void testConnectionBrokenEmail() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.getId()).thenReturn(new ObjectId());
        when(mockConnection.getAccount()).thenReturn(testAccount);
        when(mockConnection.getAlias()).thenReturn("My Test Connection");

        when(mockUserService.getAccountAdmin(testAccount)).thenReturn(testUser);

        testSenderUser.setId(new ObjectId());
        emailService.sendConnectionBrokenEmail(mockConnection);
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(mockAmazonSimpleEmailServiceClient).sendEmail(captor.capture());
        Body body = captor.getValue().getMessage().getBody();
        String htmlContent = body.getHtml().getData();
        String textContent = body.getText().getData();
        Assert.assertFalse("Encountered unexpanded template token.", htmlContent.contains("${"));
        Assert.assertFalse("Encountered unexpanded template token.", textContent.contains("${"));
    }
}
