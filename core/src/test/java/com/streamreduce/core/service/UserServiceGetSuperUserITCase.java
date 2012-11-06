package com.streamreduce.core.service;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.User;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceGetSuperUserITCase extends AbstractServiceTestCase {

    @Autowired
    UserService userService;

    @Test
    public void testGetSuperUserReturnsNewUserInstance() {
        //Tests that UserServer.getSuperUser() returns different references to equal SuperUser objects.
        User superUserA = userService.getSuperUser();
        User superUserB = userService.getSuperUser();

        Assert.assertEquals(superUserA,superUserB);
        Assert.assertNotSame(superUserA,superUserB);
    }
}
