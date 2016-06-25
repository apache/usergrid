package org.apache.usergrid.security.sso;

import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.security.tokens.TokenInfo;

import java.util.Map;

/**
 * Created by ayeshadastagiri on 6/22/16.
 */
public interface ExternalSSOProvider {

    /** Authenticate a userId and external token against this provider */
    TokenInfo validateAndReturnTokenInfo(String token, long ttl) throws Exception;

    /** Authenticate a userId and external token against this provider */
    UserInfo validateAndReturnUserInfo(String token, long ttl) throws Exception;

    /** Decode the token, if supported, and return any information encoded with the token */
    Map<String, String> getDecodedTokenDetails(String token) throws Exception;

}
