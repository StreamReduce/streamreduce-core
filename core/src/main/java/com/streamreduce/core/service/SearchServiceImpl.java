package com.streamreduce.core.service;

import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.cache.DefaultEntityCache;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.util.HTTPUtils;
import com.streamreduce.util.MessageUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A service that interfaces to operations on an ElasticSearch cluster.
 */
@Service("searchService")
public class SearchServiceImpl extends AbstractService implements  SearchService {

    private static final String RIVER_META_PATH = "/_river/%s/_meta";
    private static final Map<String, String> EMPTY_PARAMS = Collections.emptyMap();

    @Value("${message.database.name}")
    private String messageDatabaseName;
    @Value("${message.database.host}")
    private String mongoHost;
    @Value("${message.database.port}")
    private int mongoPort;

    @Value("${search.host}")
    private String elasticSearchHost;
    @Value("${search.port}")
    private int elasticSearchPort;

    @Value("${search.enabled}")
    private boolean enabled;


    /**
     * Create a Mongo River for the collection specified by the Account to Elastic Search.
     *
     * @param account - The account to create a connection for.  This must be a legit, non-null account reference with
     *                a non-null Id.
     * @return The base URL endpoint on elastic search where documents from the collection can be search on.
     */
    @Override
    public URL createRiverForAccount(Account account) {
        if (!enabled) {
            return null;
        }

        if (account == null || account.getId() == null) {
            throw new IllegalArgumentException("An account and its Id must be non-null in order to create a River");
        }

        String collectionName = MessageUtils.getMessageInboxPath(account);
        String indexName = messageDatabaseName + "_" + account.getId().toString();
        JSONObject payload = createRiverPayload(collectionName, indexName, null);
        JSONObject result = makeRequest(getRiverMetaPath(account), payload, EMPTY_PARAMS, "PUT");

        if (wasRiverCreated(result)) {
            return createBaseURLForSearchIndexAndType(account);
        } else {
            throw new RuntimeException("Unable to create Elastic Search River for account " + account + ":  " + result);
        }
    }


    /**
     * Creates rivers, in bulk, for all of the passed in accounts.  All attempted river creations fail gracefully so
     * as not to prevent rivers from being created for valid accounts.
     * @param accounts List of accounts that need rivers created for them.
     */
    @Override
    public void createRiversForAccounts(List<Account> accounts) {
        if (!enabled) { return;}

        for (Account account : accounts) {
            createRiverForAccountWithGracefulFailure(account);
        }
    }

    @Override
    public List<SobaMessage> searchMessages(Account account, String resourceName, Map<String,String> searchParameters,
                                      JSONObject query) {
        if (!enabled) {
            return Collections.emptyList();
        }

        URL searchBaseUrl = createBaseURLForSearchIndexAndType(account);
        String path = searchBaseUrl.getPath() + resourceName;
        JSONObject response = makeRequest(path,query,searchParameters,"GET");


        if (response.containsKey("_source")) {
            SobaMessage sobaMessage = createSobaMessageFromJson(response.getJSONObject("_source"));
            return Lists.newArrayList(sobaMessage);
        } else {
            JSONArray hits = response.getJSONObject("hits").getJSONArray("hits");
            List<SobaMessage> sobaMessages = new ArrayList<SobaMessage>();
            for (Object hit : hits) {
                JSONObject hitAsJson = (JSONObject) hit;
                JSONObject sobaMessageAsJson = hitAsJson.getJSONObject("_source");
                sobaMessages.add(createSobaMessageFromJson(sobaMessageAsJson));
            }
            return sobaMessages;
        }
    }

    private SobaMessage createSobaMessageFromJson(JSONObject jsonObject) {
        DBObject dbObject = BasicDBObjectBuilder.start(jsonObject).get();

        if (dbObject.containsField("details")) {
            JSONObject detailsAsJson = (JSONObject) dbObject.get("details");
            DBObject details =  BasicDBObjectBuilder.start(detailsAsJson).get();
            dbObject.put("details",details);
        }

        return (SobaMessage) new Mapper().fromDBObject(SobaMessage.class,dbObject, new DefaultEntityCache());
    }


    private void createRiverForAccountWithGracefulFailure(Account account) {
        try {
            URL searchBaseUrl = createRiverForAccount(account);
            logger.info("River for account " + account + " created/updated.  Base search url is at " + searchBaseUrl);
        } catch (Exception e) {
            //Fail gracefully, since the primary client of this method will be the bootstrap script.
            logger.error("Unable to create river for account " + account , e);
        }
    }

    private URL createBaseURLForSearchIndexAndType(Account account) {
        try {
            return new URIBuilder().setScheme("http")
                    .setHost(elasticSearchHost)
                    .setPort(elasticSearchPort)
                    .setPath("/" + messageDatabaseName.toLowerCase() + "_" + account.getId().toString() + "/")
                    .build()
                    .toURL();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean wasRiverCreated(JSONObject result) {
        return (result.has("ok") && result.getBoolean("ok"));
    }

    private String createRiverNameForAccount(Account account) {
        return messageDatabaseName.toLowerCase() + "_" + account.getId().toString();
    }

    private String getRiverMetaPath(Account account) {
        return String.format(RIVER_META_PATH, createRiverNameForAccount(account));
    }


    private JSONObject createRiverPayload(String collectionName, String index, @Nullable String type) {
        if (StringUtils.isBlank(collectionName) || StringUtils.isBlank(index) || StringUtils.isBlank(index)) {
            throw new IllegalArgumentException("JSON Payload for creating a river must not have a null or blank " +
                    "collectionName, index, or type");
        }

        JSONObject mongodbObject = new JSONObject();
        mongodbObject.put("db", messageDatabaseName);
        mongodbObject.put("host", mongoHost);
        mongodbObject.put("port", mongoPort);
        mongodbObject.put("collection", collectionName);

        //ElasticSearch indicies and types must be lower case
        JSONObject indexObject = new JSONObject();
        indexObject.put("name", index.toLowerCase());
        if (StringUtils.isNotBlank(type)) {
            indexObject.put("type", type.toLowerCase());
        }

        JSONObject createRiverPayload = new JSONObject();
        createRiverPayload.put("type", "mongodb");
        createRiverPayload.put("mongodb", mongodbObject);
        createRiverPayload.put("index", indexObject);

        return createRiverPayload;
    }

    @Override
    public JSONObject makeRequest(String path, JSONObject payload, Map<String, String> urlParameters, String method) {
        URIBuilder urlBuilder = new URIBuilder();
        urlBuilder.setScheme("http")
                .setHost(elasticSearchHost)
                .setPort(elasticSearchPort)
                .setPath(path);
        if (urlParameters != null) {
            for (String paramName : urlParameters.keySet()) {
                String value = urlParameters.get(paramName);
                if (StringUtils.isNotBlank(value)) {
                    urlBuilder.setParameter(paramName, value);
                }
            }
        }

        String url;
        try {
            url = urlBuilder.build().toString();
            String response = HTTPUtils.openUrl(url, method, payload.toString(), MediaType.APPLICATION_JSON,
                    null, null, null, null);
            return JSONObject.fromObject(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setMessageDatabaseName(String messageDatabaseName) {
        this.messageDatabaseName = messageDatabaseName;
    }

    public void setMongoHost(String mongoHost) {
        this.mongoHost = mongoHost;
    }

    public void setMongoPort(int mongoPort) {
        this.mongoPort = mongoPort;
    }

    public void setElasticSearchHost(String elasticSearchHost) {
        this.elasticSearchHost = elasticSearchHost;
    }

    public void setElasticSearchPort(int elasticSearchPort) {
        this.elasticSearchPort = elasticSearchPort;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
