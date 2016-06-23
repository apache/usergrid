package org.apache.usergrid.security.tokens.externalProviders;

import org.apache.usergrid.security.tokens.TokenInfo;

/**
 * Created by ayeshadastagiri on 6/22/16.
 */
public interface ExternalTokenProvider {

    /** Authenticate a userId and external token against this provider */
    TokenInfo validateAndReturnUserInfo(String token) throws Exception;

}
