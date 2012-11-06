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

package com.streamreduce.rest.resource;

public final class ErrorMessages {

    private ErrorMessages() {}

    public static final String APPLICATION_ACCESS_DENIED = " User does not have access to a particular application";
    public static final String EXPIRED_CREDENTIAL = " User credentials have expired";
    public static final String GROUP_NOT_FOUND = " Group is not found";
    public static final String ILLEGAL_ARGUMENT = " REST method is given an illegal argument";
    public static final String INACTIVE_ACCOUNT = " User account is inactive";
    public static final String INVALID_USER_AUTHENTICATION = " username/password combination for authentication is invalid";
    public static final String INVALID_CREDENTIAL = " The supplied credential is not valid.";
    public static final String INVALID_EMAIL = " Given email address is not valid";
    public static final String INVALID_GROUP = " Given group is invalid. E.g. unknown group type, adding a group that already exists";
    public static final String INVALID_USER = " Given user is invalid. E.g. adding a user that already exists";
    public static final String UNSUPPORTED_OPERATION = " Requested operation is not supported";
    public static final String USER_NOT_FOUND = " User not found";
    public static final String OPERATION_FAILED = " Operation failed for any other reason";
    public static final String MISSING_REQUIRED_FIELD = " Required field missing ";
    public static final String INVALID_PASSWORD_ERROR = " Passwords must be between 6 and 20 characters. ";

}
