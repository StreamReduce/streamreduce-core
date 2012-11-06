package com.streamreduce.datasource;

import com.streamreduce.Constants;
import com.streamreduce.core.ApplicationManager;
import com.streamreduce.core.dao.AccountDAO;
import com.streamreduce.core.dao.RoleDAO;
import com.streamreduce.core.dao.SystemInfoDAO;
import com.streamreduce.core.model.Account;
import com.streamreduce.core.model.Role;
import com.streamreduce.core.model.SystemInfo;
import com.streamreduce.core.model.User;
import com.streamreduce.core.service.UserService;
import com.streamreduce.core.service.exception.UserNotFoundException;
import com.streamreduce.datasource.patch.Patch;
import com.streamreduce.datasource.patch.PatchException;
import com.streamreduce.security.Permissions;
import com.streamreduce.security.Roles;
import com.streamreduce.util.JSONObjectBuilder;

import java.util.ResourceBundle;

import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The BootstrapDatabaseDataPopulator class is a Spring @see org.springframework.beans.factory.InitializingBean that will
 * bootstrap the database with the necessary data for Nodeable to run.
 *
 * @since 1.0
 */
public class BootstrapDatabaseDataPopulator implements InitializingBean, ApplicationContextAware {

    private String superUserPassword;
    private boolean bootstrapSkip;
    private String latestPatch;
    private String patchMaster;

    protected transient Logger logger = LoggerFactory.getLogger(BootstrapDatabaseDataPopulator.class);

    @Autowired
    private RoleDAO roleDAO;
    @Autowired
    private AccountDAO accountDAO;
    @Autowired
    private UserService userService;
    @Autowired
    private SystemInfoDAO systemStatusDAO;
    @Autowired
    private ApplicationManager applicationManager;


    private ApplicationContext applicationContext;

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (bootstrapSkip) {
            return;
        }

        bootstrapSystemInfo();
        doPatch();
        bootstrapRoles();
        bootstrapUsersAndAccounts();
    }

    private void bootstrapSystemInfo() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("application");
        SystemInfo systemStatus = systemStatusDAO.getLatest();
        if (systemStatus == null) {
            systemStatus = new SystemInfo();
        }
        systemStatus.setAppVersion(resourceBundle.getString("nodeable.version"));
        systemStatus.setBuildNumber(resourceBundle.getString("nodeable.build"));
        systemStatusDAO.save(systemStatus);
    }


    private void doPatch() {
        SystemInfo systemInfo = systemStatusDAO.getLatest();

        // the current patch version application is running
        Integer currentPatchLevel = systemInfo.getPatchLevel();

        // if it's not set in the db... should only ever happen once (unless you dropped your db)
        if (currentPatchLevel == null) {
            currentPatchLevel = 0;
        }

        // from the properties, this is what we should apply
        Integer latestPatchAvailable = new Integer(latestPatch);
        logger.info("[BOOTSTRAP] db is at patch level " + currentPatchLevel);
        logger.info("[BOOTSTRAP] available patch number is " + latestPatchAvailable);

        // See if there is anything to do
        if (!isPatchRequired()) {
            logger.info("[BOOTSTRAP] nothing to do, exiting doPatch()");
            return;
        }

        // check to see if this host is the PatchMaster, all others are blocked until completion
        try {
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            if (!hostname.equals(patchMaster)) {

                // sleep, waking periodically to see if the patch is done.
                while (isPatchRequired()) {
                    logger.info("[BOOTSTRAP]" + hostname + " sleeping while db is locked and patches are applied.");
                    Thread.sleep(3000); // 3 second nap
                }
                logger.info("[BOOTSTRAP]" + hostname + " waking from sleep and booting, patch master is done.");
                // done, continue the boot process
                return;
            }

        } catch (Exception e) {
            logger.error("[BOOTSTRAP] patch failure: " + e.getMessage());
            throw new RuntimeException("[BOOTSTRAP] patch failure, terminating." + e.getMessage());
        }

        // apply patches if you are the patch master
        while (currentPatchLevel < latestPatchAvailable) {
            try {
                currentPatchLevel = currentPatchLevel + 1;

                Patch patch = (Patch) applicationContext.getBean("patch" + currentPatchLevel);
                if (patch == null) {
                    logger.info("[BOOTSTRAP] class not found for patch" + currentPatchLevel);
                    logger.info("[BOOTSTRAP] patch " + currentPatchLevel + " not applied, exiting patch process");
                    break;
                }

                patch.applyPatch(applicationManager, applicationContext);

                // if it fails do no set this.. there is no "continue" here.
                systemInfo.setPatchLevel(currentPatchLevel);
                systemStatusDAO.save(systemInfo);

            } catch (PatchException e) {
                logger.info("[BOOTSTRAP] " + e.getMessage() + " patch " + currentPatchLevel + " not applied, exiting patch process");
                break;
            }
            logger.info("[BOOTSTRAP] patch " + currentPatchLevel + " successfully applied");
        }
    }

    private boolean isPatchRequired() {
        SystemInfo systemInfo = systemStatusDAO.getLatest();
        Integer currentPatchLevel = systemInfo.getPatchLevel();
        return currentPatchLevel == null || currentPatchLevel < new Integer(latestPatch);
    }


    /**
     * Bootstraps the necessary user accounts.
     */
    private void bootstrapUsersAndAccounts() {

        // create it if it doesn't exist
        // always create the system user first
        if (getUser(Constants.NODEABLE_SYSTEM_USERNAME) == null) {

            Account rootAccount = accountDAO.findByName(Constants.NODEABLE_SUPER_ACCOUNT_NAME);

            // create it if it doesn't exist, should only happen with a clean db
            if (rootAccount == null) {
                Account account = new Account.Builder()
                        .url("http://nodeable.com")
                        .description("Core Nodeable Account")
                        .name(Constants.NODEABLE_SUPER_ACCOUNT_NAME)
                        .build();
                rootAccount = userService.createAccount(account);
            }

            JSONObject config = new JSONObjectBuilder().add("icon", "/images/nodebelly.jpg").build();

            // we are bypassing the lifecycle here, but still firing proper events
            User user = new User.Builder()
                    .username(Constants.NODEABLE_SYSTEM_USERNAME)
                    .password(superUserPassword)
                    .accountLocked(false)
                    .fullname("Nodeable")
                    .userStatus(User.UserStatus.ACTIVATED)
                    .roles(userService.getAdminRoles())
                    .account(rootAccount)
                    .alias("nodeable")
                    .userConfig(config)
                    .build();

            userService.createUser(user);
        }

        // create it if it doesn't exist
        if (getUser(Constants.NODEABLE_SUPER_USERNAME) == null) {

            JSONObject config = new JSONObjectBuilder().add("icon", "/images/nodebelly.jpg").build();

            // we are bypassing the lifecycle here, but still firing proper events
            User user = new User.Builder()
                    .username(Constants.NODEABLE_SUPER_USERNAME)
                    .password(superUserPassword)
                    .accountLocked(false)
                    .fullname("Nodeable Insight")
                    .userStatus(User.UserStatus.ACTIVATED)
                    .roles(userService.getAdminRoles())
                    .account(accountDAO.findByName(Constants.NODEABLE_SUPER_ACCOUNT_NAME))
                    .alias("insight")
                    .userConfig(config)
                    .build();

            userService.createUser(user);
        }

    }

    public void bootstrapMinimumData() {
        bootstrapSystemInfo();
        bootstrapRoles();
        bootstrapUsersAndAccounts();
    }

    private User getUser(String username) {
        try {
            return userService.getUser(username);
        } catch (UserNotFoundException unfe) {
            return null;
        }
    }

    /**
     * Bootstrap the initial Roles and their Permissions.
     */
    private void bootstrapRoles() {
        if (roleDAO.findRole(Roles.ADMIN_ROLE) == null) {
            Role admin = new Role(Roles.ADMIN_ROLE, "Administrator Role");
            admin.setPermissions(Permissions.ALL);
            roleDAO.save(admin);
        }
        if (roleDAO.findRole(Roles.DEVELOPER_ROLE) == null) {
            Role dev = new Role(Roles.DEVELOPER_ROLE, "Developer Role");
            dev.addPermissions(Permissions.APP_USER);
            roleDAO.save(dev);
        }
        if (roleDAO.findRole(Roles.USER_ROLE) == null) {
            Role user = new Role(Roles.USER_ROLE, "Required Role to Login");
            user.addPermissions(Permissions.APP_USER);
            roleDAO.save(user);
        }
    }

    public void setSuperUserPassword(String superUserPassword) {
        this.superUserPassword = superUserPassword;
    }

    public void setLatestPatch(String latestPatch) {
        this.latestPatch = latestPatch;
    }

    public void setBootstrapSkip(boolean bootstrapSkip) {
        this.bootstrapSkip = bootstrapSkip;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setPatchMaster(String patchMaster) {
        this.patchMaster = patchMaster;
    }
}
