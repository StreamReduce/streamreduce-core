package com.streamreduce.security.realm;

import com.streamreduce.connections.GatewayProvider;
import com.streamreduce.core.model.Connection;
import com.streamreduce.core.service.SecurityService;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.security.token.GatewayAuthenticaionToken;
import com.streamreduce.security.token.NodeableAuthenticationToken;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class GatewayTokenAuthorizingRealm extends NodeableAuthorizingRealm {

    @Autowired
    private SecurityService securityService;
    private transient Logger logger = LoggerFactory.getLogger(getClass());

    public GatewayTokenAuthorizingRealm() {
        setAuthenticationTokenClass(GatewayAuthenticaionToken.class);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {

        NodeableAuthenticationToken token = (GatewayAuthenticaionToken) authcToken;

        logger.debug("Attempting to get gateway api authentication info for" + ((GatewayAuthenticaionToken) authcToken).getToken());

        Connection connection = securityService.getByApiKey(token.getToken(), GatewayProvider.TYPE);

        if (connection == null) {
            throw new AuthenticationException(ErrorMessages.INVALID_CREDENTIAL);
        }

        logger.debug("ConnectionId is set to " + connection.getId());

        // all is well so far...
        return new SimpleAuthenticationInfo(connection.getCredentials().getIdentity(), "", getName());
    }


    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return null;
    }

}
