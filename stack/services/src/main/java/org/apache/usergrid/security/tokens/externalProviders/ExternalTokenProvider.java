package org.apache.usergrid.security.tokens.externalProviders;

import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.security.tokens.TokenInfo;

/**
 * Created by ayeshadastagiri on 6/22/16.
 */
public interface ExternalTokenProvider {

    /** Authenticate a userId and external token against this provider */
    TokenInfo validateAndReturnTokenInfo(String token, long ttl) throws Exception;

    /** Authenticate a userId and external token against this provider */
    UserInfo validateAndReturnUserInfo(String token, long ttl) throws Exception;

}
