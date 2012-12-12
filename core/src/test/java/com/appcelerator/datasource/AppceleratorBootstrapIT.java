package com.appcelerator.datasource;

import com.streamreduce.AbstractServiceTestCase;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;


public class AppceleratorBootstrapIT extends AbstractServiceTestCase {

    private static final String APPC_GENERIC_USER_EMAIL_ADDRESS = "generic-streamreduce-user@appcelerator.com";

    @Autowired
    AppceleratorBootstrap appceleratorBootstrap;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        appceleratorBootstrap.bootstrapAccountAndUser();  //bootstrapping is skipped, so call this manually.
    }

    @Test
    public void testBootstrapCreatesAccountAndUser() throws Exception {
        User user = userService.getUser(APPC_GENERIC_USER_EMAIL_ADDRESS);
        Account account = user.getAccount();

        assertEquals("Appcelerator Generic Account", account.getName());

        assertEquals("generic_appcelerator_user", user.getAlias());
        assertEquals("Generic Appcelerator User", user.getFullname());
        assertEquals(userService.getUserRoles(), user.getRoles());
        assertEquals(User.UserStatus.ACTIVATED, user.getUserStatus());
    }

    @Test
    public void testBootstrapCreatesDoesNotGetCreatedSecondTime() throws Exception {
        //Ensure no calls to createUser or createAccount occur after the first bootstrap in setUp succeeds.
        UserService userService = mock(UserService.class);
        ReflectionTestUtils.setField(appceleratorBootstrap,"userService",userService);
        appceleratorBootstrap.bootstrapAccountAndUser();
        verifyZeroInteractions(userService);

    }


}
