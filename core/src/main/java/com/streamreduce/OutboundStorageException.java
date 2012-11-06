package com.streamreduce;

/**
 * Exception that occurs when an attempt to store a payload via the Outbound Message Gateway fails.  Instances of this
 * Exception should signal failure and trigger some form of retry to the outbound storage, but should not terminate the
 * execution path of logic that triggered persisting to the Outbound Message Gateway.
 *
 */
public class OutboundStorageException extends NodeableException {

    @SuppressWarnings("unused")
    public OutboundStorageException(String s) {
        super("OutboundStorage Exception:" + s);
    }

    @SuppressWarnings("unused")
    public OutboundStorageException(String s, Throwable t) {
        super("OutboundStorage Exception:" + s,t);
    }
}
