package com.streamreduce.core.service;

import com.streamreduce.AbstractServiceTestCase;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import org.junit.Test;

/**
 * Integration test that exercises end to end usage of User.userConfig for CRUD operations on
 * a User.
 */
public class UserServiceUserConfigCrudITCase extends AbstractServiceTestCase {

    @Test
    public void testSaveAndReadUserObjectWithNullValueInConfig() throws Exception {
        //Test that a JSONArray with a null value doesn't cause deserialization problems when going back/forth
        //from mongo
        JSONArray arr = new JSONArray();
        arr.add(JSONNull.getInstance());

        testUser.setConfigValue("watchlist",arr);
        userService.updateUser(testUser);
        userService.getUser(testUser.getUsername());


    }
}
