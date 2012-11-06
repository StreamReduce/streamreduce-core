package com.streamreduce.rest;

import com.streamreduce.AbstractInContainerTestCase;
import com.streamreduce.core.dao.SobaMessageDAO;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageComment;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.rest.dto.response.MessageCommentsResponseDTO;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import com.streamreduce.util.JSONObjectBuilder;
import net.sf.json.JSONObject;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MessageResourceITCase extends AbstractInContainerTestCase {

    @Autowired
    private SobaMessageDAO sobaMessageDAO;

    private String authToken;
    private SobaMessageResponseDTO userMessage;
    private User localUser1;
    private User localUser2;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        localUser1 = new User.Builder()
                .username("local_test_user_1@nodeable.com")
                .password("local_test_user_1@nodeable.com")
                .accountLocked(false)
                .fullname("Local Test User 1")
                .userStatus(User.UserStatus.ACTIVATED)
                .account(testAccount)
                .roles(testUser.getRoles())
                .accountOriginator(false)
                .alias("LocalUser1")
                .build();

        localUser2 = new User.Builder()
                .username("local_test_user_2@nodeable.com")
                .password("local_test_user_2@nodeable.com")
                .accountLocked(false)
                .fullname("Local Test User 2")
                .userStatus(User.UserStatus.ACTIVATED)
                .account(testAccount)
                .roles(testUser.getRoles())
                .accountOriginator(false)
                .alias("LocalUser2")
                .build();

        localUser1 = userService.createUser(localUser1);
        localUser2 = userService.createUser(localUser2);

        // login
        authToken = login(testUsername, testUsername);

        try {
            createUserMessage();
        } catch (Exception e) {
            if (localUser1 != null) {
                userService.deleteUser(localUser1);
            }
            if (localUser2 != null) {
                userService.deleteUser(localUser2);
            }
            e.printStackTrace();
        }
    }


    protected String getUrl() {
        return getPublicApiUrlBase() + "/messages";
    }

    protected void createUserMessage() throws Exception {
        JSONObject json = new JSONObject();

        json.put("message", "This is a bootstrapped message to self and the stream");

        // not such a good idea.
        SobaMessageResponseDTO message = jsonToObject(makeRequest(getUrl(), "POST", json, authToken),
                TypeFactory.defaultInstance().constructType(SobaMessageResponseDTO.class));

        userMessage = message;
    }


    @Test
    @Ignore
    public void testGetAllMessages() throws Exception {
        String response = makeRequest(getUrl(), "GET", null, authToken);
        List<SobaMessageResponseDTO> messages = jsonToObject(response, TypeFactory.defaultInstance().constructCollectionType(List.class,
                SobaMessageResponseDTO.class));
        assertNotNull(messages);

    }


    @Test
    @Ignore
    public void testGetAllMessagesNotLoggedIn() throws Exception {
        String request = makeRequest(getUrl(), "GET", null, null);
        assertTrue(request.contains("No credentials set"));
    }

    @Test
    @Ignore
    public void testAddMessage() throws Exception {
        JSONObject json = new JSONObject();

        json.put("message", "This is a message with no @");

        SobaMessageResponseDTO message = jsonToObject(makeRequest(getUrl(), "POST", json, authToken),
                TypeFactory.defaultInstance().constructType(SobaMessageResponseDTO.class));

        assertNotNull(message);
        assertEquals(getTestUser().getId(), message.getSenderId());
        //
    }

    @Test
    @Ignore
    public void testAddMessageToAnotherUser() throws Exception {
        JSONObject json = new JSONObject();

        json.put("message", "This is a message to @" + localUser1.getAlias());

        SobaMessageResponseDTO message = jsonToObject(makeRequest(getUrl(), "POST", json, authToken),
                TypeFactory.defaultInstance().constructType(SobaMessageResponseDTO.class));

        assertNotNull(message);
        assertEquals(getTestUser().getId(), message.getSenderId());
        //
    }

    @Test
    @Ignore
    public void testDeleteMessageToAnotherUser() throws Exception {
        JSONObject json = new JSONObject();

        json.put("message", "This is a message to @" + localUser1.getAlias());

        SobaMessageResponseDTO message = jsonToObject(makeRequest(getUrl(), "POST", json, authToken),
                TypeFactory.defaultInstance().constructType(SobaMessageResponseDTO.class));

        assertNotNull(message);
        assertEquals(getTestUser().getId(), message.getSenderId());
        //
        // mock remove it
        makeRequest(getUrl() + "/" + message.getId(), "DELETE", json, authToken);

        SobaMessageResponseDTO responseDTO = jsonToObject(makeRequest(getUrl() + "/" + message.getId(), "GET",
                null, authToken),
                TypeFactory.defaultInstance().constructType(SobaMessageResponseDTO.class));

        assertEquals(responseDTO.getPayload(), "message removed by @" + testUser.getAlias());

    }


    @Test
    @Ignore
    public void testGetPrivateMessageById() throws Exception {
        // get one random message
        SobaMessage privateMessage = applicationManager.getMessageService().getAllMessages(getTestUser(), null, null, 1, true, null, null, null, false).get(0);
        SobaMessageResponseDTO responseDTO = jsonToObject(makeRequest(getUrl() + "/" + privateMessage.getId(), "GET",
                null, authToken),
                TypeFactory.defaultInstance().constructType(SobaMessageResponseDTO.class));

        assertNotNull(responseDTO);
        assertEquals(privateMessage.getId(), responseDTO.getMessageId());
    }

    @Test
    @Ignore
    public void testAddCommentToMessage() throws Exception {

        // get the message
        SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        assertEquals(0, sobaMessage.getComments().size());

        JSONObject json = new JSONObject();

        json.put("comment", "Some Dumb Comment");

        String url = getUrl() + "/" + userMessage.getMessageId() + "/comment";
        makeRequest(url, "PUT", json, authToken);

        sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        assertEquals(1, sobaMessage.getComments().size());

    }


    @Test
    @Ignore
    public void testDeleteCommentOnMessage() throws Exception {

        // get the message
        SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        assertEquals(0, sobaMessage.getComments().size());

        JSONObject json = new JSONObject();

        json.put("comment", "Some Dumb Comment");

        String url = getUrl() + "/" + userMessage.getMessageId() + "/comment";
        makeRequest(url, "PUT", json, authToken);

        sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        assertEquals(1, sobaMessage.getComments().size());
        MessageComment messageComment =  sobaMessage.getComments().get(0);

        // now remove (nullify) the comment
        makeRequest(url + "/" + messageComment.getCreated(), "DELETE", json, authToken);

        sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        assertEquals(sobaMessage.getComments().get(0).getComment(), "comment removed by @" + testUser.getAlias());

    }

    @Test
    @Ignore
    public void testGetCommentsForMessage() throws Exception {

        int num = 0;
        SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        sobaMessage.addComment(new MessageComment(getTestUser(), "Some comment from test user"));
        num = sobaMessage.getComments().size();
        applicationManager.getMessageService().updateMessage(getTestUser().getAccount(), sobaMessage);

        String url = getUrl() + "/" + userMessage.getMessageId() + "/comment";
        String response = makeRequest(url, "GET", null, authToken);

        MessageCommentsResponseDTO commentsResponseDTO = jsonToObject(response,
                TypeFactory.defaultInstance().constructType(MessageCommentsResponseDTO.class));

        assertEquals(num, commentsResponseDTO.getComments().size());

    }

    @Test
    @Ignore
    public void testGetTags() throws Exception {
        SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        int num = sobaMessage.getHashtags().size();

        String url = getUrl() + "/" + userMessage.getMessageId() + "/hashtag";
        String response = makeRequest(url, "GET", null, authToken);

        Set<String> tags = jsonToObject(response, TypeFactory.defaultInstance().constructCollectionType(Set.class,
                String.class));

        assertEquals(num, tags.size());

    }

    @Test
    @Ignore
    public void testAddTag() throws Exception {

        SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        int num = sobaMessage.getHashtags().size();

        String url = getUrl() + "/" + userMessage.getMessageId() + "/hashtag";
        JSONObject json = new JSONObject();

        json.put("hashtag", "#foo");

        makeRequest(url, "POST", json, authToken);

        sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        assertEquals(num + 1, sobaMessage.getHashtags().size());

        String response = makeRequest(getUrl() + "/" + userMessage.getMessageId(), "GET", json, authToken);
        SobaMessageResponseDTO sobaMessageResponseDTO = jsonToObject(response,
                TypeFactory.defaultInstance().constructType(SobaMessageResponseDTO.class));

        assertEquals(num + 1, sobaMessageResponseDTO.getHashtags().size());

    }


    @Test
    @Ignore
    public void testRemoveTag() throws Exception {
        SobaMessage sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        sobaMessage.addHashtag("foo");

        int num = sobaMessage.getHashtags().size();

        applicationManager.getMessageService().updateMessage(getTestUser().getAccount(), sobaMessage);

        String url = getUrl() + "/" + userMessage.getMessageId() + "/hashtag/foo";
        makeRequest(url, "DELETE", null, authToken);

        sobaMessage = applicationManager.getMessageService().getMessage(getTestUser().getAccount(),
                userMessage.getMessageId());

        assertEquals(num - 1, sobaMessage.getHashtags().size());
    }


    @Test
    @Ignore
    public void testEmail() throws Exception {
        JSONObject json = new JSONObjectBuilder()
                .add("recipient", "integrations@nodeable.com")
                .add("subject", "Testing email endpoint - subject")
                .add("body", "Testing email endpoint - body")
                .build();
        String url = getUrl() + "/" + userMessage.getMessageId() + "/email";
        makeRequest(url, "POST", json, authToken);
    }


}
