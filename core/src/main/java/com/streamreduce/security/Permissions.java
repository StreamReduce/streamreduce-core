package com.streamreduce.security;

import java.util.HashSet;
import java.util.Set;

public class Permissions {

    public static final String APP_USER = "APP_USER";
    public static final String ITEM_READ = "READ_ITEM";
    public static final String ITEM_MODIFY = "MODIFY_ITEM";
    public static final String ITEM_DELETE = "DELETE_ITEM";

    public static final Set<String> ALL = new HashSet<String>();

    static {
        ALL.add(APP_USER);
        ALL.add(ITEM_READ);
        ALL.add(ITEM_MODIFY);
        ALL.add(ITEM_DELETE);
    }

}
