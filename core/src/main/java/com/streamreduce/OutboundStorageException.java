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
