package com.streamreduce.client.outbound;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import com.streamreduce.core.model.messages.MessageType;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import com.streamreduce.test.service.TestUtils;
import com.streamreduce.util.JSONObjectBuilder;
import com.streamreduce.util.WebHDFSClient;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WebHDFSOutboundClientTestIT extends AbstractServiceTestCase {

    @Value("${webhdfs.host}")
    private String host;
    @Value("${webhdfs.port}")
    private String port;
    private String testFileUrl = String.format("http://%s:%s/webhdfs/v1/testWebHDFSClientIT%s", host, port, System.currentTimeMillis());

    private Connection testConnection;
    private OutboundConfiguration outboundConfiguration;
    private WebHDFSOutboundClient outboundClient;
    private WebHDFSClient webHDFSClient;


    @Before
    public void setUp() throws Exception {

        testConnection = TestUtils.createCloudConnection();

        outboundConfiguration = new OutboundConfiguration.Builder()
                .protocol("webhdfs")
                .destination(testFileUrl)
                .credentials(new ConnectionCredentials("hadoop", null))
                .dataTypes(OutboundDataType.RAW, OutboundDataType.EVENT, OutboundDataType.RAW,
                        OutboundDataType.PROCESSED)
                .originatingConnection(testConnection)
                .build();
        outboundClient = new WebHDFSOutboundClient(outboundConfiguration);
        webHDFSClient = new WebHDFSClient(outboundConfiguration);
        webHDFSClient.mkdirs("");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSendRawMessage() throws Exception {
        //Send a JSONObject to sendRawMessage... just verify that raw was created.
        JSONObject rawMessage = new JSONObjectBuilder().add("foo", "bar").build();
        outboundClient.putRawMessage(rawMessage);
        assertTrue(webHDFSClient.exists("raw"));
    }

    @Test
    public void testSendProcessedMessage() throws Exception {
        //Verify that we create a file on WebHDFS named processed/connId/messageId

        SobaMessage sobaMessage = new SobaMessage.Builder()
                .sender(createTestUser())
                .providerId("test")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .transformedMessage("Foo")
                .type(MessageType.USER)
                .connection(testConnection)
                .build();
        sobaMessage.setId(new ObjectId());

        outboundClient.putProcessedMessage(sobaMessage);
        String expectedPath = "processed/" + sobaMessage.getConnectionId() + "/" + sobaMessage.getId();
        byte[] payload = webHDFSClient.readFile(expectedPath);
        assertTrue(payload.length > 0);

        String actualAsString = new String(payload);
        JSONObject actual = JSONObject.fromObject(actualAsString);
        String expectedSobaMessageAsString = new ObjectMapper().writeValueAsString(SobaMessageResponseDTO.fromSobaMessage(sobaMessage));
        JSONObject expected = JSONObject.fromObject(expectedSobaMessageAsString);

        assertEquals(actual, expected);
    }

    @Test
    public void testSendInsightMessage() throws Exception {
        //Verify that we create a file on WebHDFS named insight/connId/messageId

        SobaMessage sobaMessage = new SobaMessage.Builder()
                .sender(createTestUser())
                .providerId("test")
                .visibility(SobaObject.Visibility.ACCOUNT)
                .transformedMessage("Foo")
                .type(MessageType.NODEBELLY)
                .connection(testConnection)
                .build();
        sobaMessage.setId(new ObjectId());

        outboundClient.putInsightMessage(sobaMessage);
        String expectedPath = "insight/" + sobaMessage.getConnectionId() + "/" + sobaMessage.getId();

        byte[] payload = webHDFSClient.readFile(expectedPath);
        assertTrue(payload.length > 0);

        String actualAsString = new String(payload);
        JSONObject actual = JSONObject.fromObject(actualAsString);
        String expectedSobaMessageAsString = new ObjectMapper().writeValueAsString(SobaMessageResponseDTO.fromSobaMessage(sobaMessage));
        JSONObject expected = JSONObject.fromObject(expectedSobaMessageAsString);

        assertEquals(actual, expected);
    }

    private static User createTestUser() {
        Account account = new Account.Builder().name("tool").build();
        account.setId(new ObjectId());

        User user = new User.Builder()
                .username("maynard@toolband.com")
                .account(account)
                .password("trollolol")
                .fullname("Maynard James Keenan")
                .build();
        user.setId(new ObjectId());

        return user;
    }
}
