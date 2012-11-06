package com.streamreduce.test.service;

import com.streamreduce.connections.AuthType;
import com.streamreduce.connections.ConnectionProvidersForTests;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.model.ConnectionCredentials;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.OutboundDataType;
import com.streamreduce.core.model.SobaObject;
import com.streamreduce.core.model.User;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;

import java.util.ResourceBundle;


/**
 * Collection of static utility methods to create various common model objects.
 */
public class TestUtils {

    private static final String SAMPLE_FEED_FILE_PATH = TestUtils.class.getResource(
            "/com/streamreduce/rss/sample_EC2.rss").toString();


    public static User createTestUser() {
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

    public static JSONObject createValidSampleIMGPayload() {
        JSONObject json = new JSONObject();
        json.put("name", "generic_test"); // matches connection alias
        json.put("message", "four score and seven years ago...");

        String[] tags = new String[3];
        tags[0] = "#foo";
        tags[1] = "#bar";
        tags[2] = "#baz";
        json.put("tags", tags);

        JSONObject metric = new JSONObject();
        metric.put("name", "metric1");
        metric.put("type", "ABSOLUTE");
        metric.put("value", 42);

        JSONArray metrics = new JSONArray();
        json.put("metrics", metrics);

        return json;
    }

    public static Connection createFeedConnectionWithSpecificOutboundDatatypes(OutboundDataType... outboundDataTypes) {
        Connection.Builder cb = new Connection.Builder()
                .authType(AuthType.NONE)
                .alias("testFeedConnection")
                .provider(ConnectionProvidersForTests.RSS_PROVIDER)
                .url(SAMPLE_FEED_FILE_PATH)
                .user(createTestUser())
                .visibility(SobaObject.Visibility.ACCOUNT);

        if (outboundDataTypes.length > 0) {
            cb.outboundConfigurations(
                    new OutboundConfiguration.Builder()
                            .credentials(createConnectionCredentialsForAWS())
                            .dataTypes(outboundDataTypes)
                            .protocol("s3")
                            .build()
            ) ;
        }

        Connection c = cb.build();
        c.setId(new ObjectId());

        return c;
    }


    public static Connection createIMGConnectionWithSpecificOutboundDatatypes(OutboundDataType... outboundDataTypes) {
        Connection.Builder cb = new Connection.Builder()
                .authType(AuthType.API_KEY)
                .alias("testIMGConnection")
                .provider(ConnectionProvidersForTests.CUSTOM_PROVIDER)
                .url(SAMPLE_FEED_FILE_PATH)
                .user(createTestUser())
                .visibility(SobaObject.Visibility.ACCOUNT);

        if (outboundDataTypes.length > 0) {
            cb.outboundConfigurations(
                    new OutboundConfiguration.Builder()
                            .credentials(createConnectionCredentialsForAWS())
                            .dataTypes(outboundDataTypes)
                            .protocol("s3")
                            .build()
            ) ;
        }

        Connection connection = cb.build();
        connection.setId(new ObjectId());
        return cb.build();
    }

    public static ConnectionCredentials createConnectionCredentialsForAWS() {
        ResourceBundle cloudProps = ResourceBundle.getBundle("cloud");
        String accessKeyId = cloudProps.getString("nodeable.aws.accessKeyId");
        String secretKey = cloudProps.getString("nodeable.aws.secretKey");
        return new ConnectionCredentials(accessKeyId,secretKey);
    }

    public static Connection createCloudConnection() {
        Connection c = new Connection.Builder()
                .credentials(createConnectionCredentialsForAWS())
                .description("This is Nodeable's AWS cloud.")
                .alias("Nodeable Cloud")
                .provider(ConnectionProvidersForTests.AWS_CLOUD_PROVIDER)
                .user(createTestUser())
                .authType(AuthType.USERNAME_PASSWORD)
                .build();

        c.setId(new ObjectId());

        return c;
    }
}
