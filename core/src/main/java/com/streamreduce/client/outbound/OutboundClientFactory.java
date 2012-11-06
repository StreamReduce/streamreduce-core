package com.streamreduce.client.outbound;

import com.streamreduce.core.model.OutboundConfiguration;
import org.springframework.stereotype.Service;

@Service
public class OutboundClientFactory {

    public OutboundClient createOutboundClientForOutboundConfiguration(OutboundConfiguration outboundConfiguration) {
        if (outboundConfiguration.getProtocol().equals("s3")) {
            return createS3OutboundClient(outboundConfiguration);
        }
        else if (outboundConfiguration.getProtocol().equals("webhdfs")) {
            return createWebHDFSOutboundClient(outboundConfiguration);
        }

        throw new IllegalArgumentException("OutboundConfiguration does not have a Connection provider and protocol " +
                "property value that can be used to generate an OutboundClient");
    }

    private WebHDFSOutboundClient createWebHDFSOutboundClient(OutboundConfiguration outboundConfiguration) {
        return new WebHDFSOutboundClient(outboundConfiguration);
    }

    private S3OutboundClient createS3OutboundClient(OutboundConfiguration outboundConfiguration) {
        return new S3OutboundClient(outboundConfiguration);
    }

}
