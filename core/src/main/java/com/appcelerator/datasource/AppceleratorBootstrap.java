package com.appcelerator.datasource;


import com.streamreduce.core.dao.AccountDAO;
import com.streamreduce.core.dao.UserDAO;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import static com.appcelerator.Constants.*;

public class AppceleratorBootstrap implements InitializingBean {

    @Autowired
    UserService userService;
    @Autowired
    AccountDAO accountDAO;
    @Autowired
    UserDAO userDAO;

    private boolean bootstrapSkip;


    @Override
    public void afterPropertiesSet() throws Exception {
        if (!bootstrapSkip) {
            bootstrapAccountAndUser();
        }


    }

    public void bootstrapAccountAndUser() {
        Account appceleratorGenericAccount = accountDAO.findByName(APPC_GENERIC_ACCOUNT_NAME);
        if (appceleratorGenericAccount == null) {
            appceleratorGenericAccount = userService.createAccount(
                    new Account.Builder()
                            .name(APPC_GENERIC_ACCOUNT_NAME)
                            .build());
        }

        if (userDAO.findUserInAccount(appceleratorGenericAccount, APPC_GENERIC_USERNAME) == null) {
            User user = new User.Builder()
                    .account(appceleratorGenericAccount)
                    .username(APPC_GENERIC_USERNAME)
                    .alias(APPC_GENERIC_ALIAS)
                    .fullname(APPC_GENERIC_FULLNAME)
                    .roles(userService.getUserRoles())
                    .userStatus(User.UserStatus.ACTIVATED)
                    .build();

            userService.createUser(user);
        }
    }

    @SuppressWarnings("unused")
    public boolean isBootstrapSkip() {
        return bootstrapSkip;
    }

    @SuppressWarnings("unused")
    public void setBootstrapSkip(boolean bootstrapSkip) {
        this.bootstrapSkip = bootstrapSkip;
    }
}
