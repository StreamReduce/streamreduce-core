package com.streamreduce.security.filter;

import com.streamreduce.Constants;
import com.streamreduce.rest.resource.ErrorMessages;
import com.streamreduce.security.token.GatewayAuthenticaionToken;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;

public class GatewayTokenAuthenticatingFilter extends NodeableAuthenticatingFilter {

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        String token = getHeaderParameter(request);
        if (token == null) {
            logger.debug("Header Authorization token is null, throw exception.");
            throw new AuthenticationException(ErrorMessages.INVALID_CREDENTIAL);
        }
        return new GatewayAuthenticaionToken(token);
    }


    @Override
    protected String getHeaderParameter(ServletRequest request) {
        return ((HttpServletRequest) request).getHeader(Constants.NODEABLE_API_KEY);
    }

}

