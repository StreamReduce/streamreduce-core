package com.streamreduce.security.realm;

import com.streamreduce.core.model.User;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.security.token.NodeableAuthenticationToken;
import com.streamreduce.security.token.UserAuthenticationToken;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public final class UserTokenAuthorizingRealm extends NodeableAuthorizingRealm {

    @Autowired
    private SecurityService securityService;

    public UserTokenAuthorizingRealm() {
        setAuthenticationTokenClass(UserAuthenticationToken.class);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {

        NodeableAuthenticationToken token = (UserAuthenticationToken) authcToken;

        logger.debug("Attempting to get authentication info for" + ((UserAuthenticationToken) authcToken).getToken());

        User theUser = securityService.getUserFromAuthenticationToken(token.getToken());

        if (theUser == null) {
            throw new AuthenticationException(ErrorMessages.INVALID_CREDENTIAL);
        }

        logger.debug("UserId is set to " + theUser.getUser().getId());

        // token is expired
//        if (userToken.getExpirationDate() < System.currentTimeMillis()) {
//            throw new AuthenticationException(ErrorMessages.EXPIRED_CREDENTIAL);
//        }
        // all is well so far...

        return new SimpleAuthenticationInfo(theUser.getId(), "", getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return super.doGetAuthorizationInfo(principals);
    }

}

