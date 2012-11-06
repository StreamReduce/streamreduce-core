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

package com.streamreduce.core.service.exception;

public class InvalidCredentialsException extends Exception {

    private static final long serialVersionUID = -5690155104176372438L;

    public InvalidCredentialsException(String msg) {
        super(msg);
    }

    public InvalidCredentialsException(Throwable cause) {
        super("The credentials associated with this connection are invalid: " + cause.getMessage(), cause);
    }

}
