package com.streamreduce.client.outbound;

import com.streamreduce.OutboundStorageException;
import com.streamreduce.core.model.OutboundConfiguration;
import com.streamreduce.core.model.messages.SobaMessage;
import com.streamreduce.rest.dto.response.SobaMessageResponseDTO;
import com.streamreduce.util.WebHDFSClient;
import net.sf.json.JSONObject;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebHDFSOutboundClient extends AbstractOutboundClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebHDFSOutboundClient.class);

    WebHDFSOutboundClient(OutboundConfiguration outboundConfiguration) {
        super(outboundConfiguration);
    }

    @Override
    public void putRawMessage(JSONObject payload) throws OutboundStorageException {
        if (MapUtils.isEmpty(payload)) {
            throw new IllegalArgumentException("Payload must be non-empty.");
        }

        try {
            byte[] payloadAsBytes = payload.toString().getBytes();
            String filepath = String.format("%s%s-%s", RAW_PAYLOAD_PREFIX, System.currentTimeMillis(), payload.hashCode());
            sendPayloadToWebHDFS(filepath,payloadAsBytes);
        }
        catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void putProcessedMessage(SobaMessage sobaMessage) throws OutboundStorageException {
        if (sobaMessage == null) {
            throw new IllegalArgumentException("sobaMessage  must be non-null");
        }

        try {
            byte[] payload = convertSobaMessageToDTOAsBytes(sobaMessage);
            String filepath = createProcessedMessagePath(sobaMessage);
            sendPayloadToWebHDFS(filepath, payload);
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void putProcessedMessage(SobaMessageResponseDTO sobaMessage) throws OutboundStorageException {
        if (sobaMessage == null) {
            throw new IllegalArgumentException("sobaMessage  must be non-null");
        }

        try {
            byte[] payload = convertSobaMessageToDTOAsBytes(sobaMessage);
            String filepath = createProcessedMessagePath(sobaMessage);
            sendPayloadToWebHDFS(filepath,payload);
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void putInsightMessage(SobaMessage sobaMessage) throws OutboundStorageException {
        if (sobaMessage == null) {
            throw new IllegalArgumentException("sobaMessage  must be non-null");
        }

        try {
            byte[] payload = convertSobaMessageToDTOAsBytes(sobaMessage);
            String filepath = createInsightMessagePath(sobaMessage);
            sendPayloadToWebHDFS(filepath,payload);
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void putInsightMessage(SobaMessageResponseDTO sobaMessage) throws OutboundStorageException {
        if (sobaMessage == null) {
            throw new IllegalArgumentException("sobaMessage  must be non-null");
        }

        try {
            byte[] payload = convertSobaMessageToDTOAsBytes(sobaMessage);
            String filepath = createInsightMessagePath(sobaMessage);
            sendPayloadToWebHDFS(filepath,payload);
        } catch (Exception e) {
            throw new OutboundStorageException(e.getMessage(), e);
        }
    }

    private void createDestinationDirectory(WebHDFSClient webHDFSClient, String directory) throws IOException {
        if (!webHDFSClient.exists(directory)) {
            if (!webHDFSClient.mkdirs(directory)) {
                throw new IOException(String.format("Unable to create destination directory %s", directory));
            }
        }
    }

    private void sendPayloadToWebHDFS(String filepath, byte[] payload)  throws IOException {
        WebHDFSClient webHDFSClient = new WebHDFSClient(outboundConfiguration);
        createDestinationDirectory(webHDFSClient, filepath.substring(0,filepath.lastIndexOf("/")));
        webHDFSClient.createFile(filepath, payload);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Message sent to WebHDFS destination at path: " + filepath);
        }
    }
}
