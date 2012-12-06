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

package com.streamreduce.security;

import java.util.HashSet;
import java.util.Set;

public class Permissions {

    public static final String APP_USER = "APP_USER";
    public static final String ITEM_READ = "READ_ITEM";
    public static final String ITEM_MODIFY = "MODIFY_ITEM";
    public static final String ITEM_DELETE = "DELETE_ITEM";

    public static final Set<String> ALL = new HashSet<>();

    static {
        ALL.add(APP_USER);
        ALL.add(ITEM_READ);
        ALL.add(ITEM_MODIFY);
        ALL.add(ITEM_DELETE);
    }

}
