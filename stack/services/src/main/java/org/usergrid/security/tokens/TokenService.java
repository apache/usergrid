package org.usergrid.security.tokens;


import java.util.Map;

import org.usergrid.security.AuthPrincipalInfo;


public interface TokenService {

    /**
     * Create the token with the given duration.  A duration value of 0 equals the default value specified in the
     * properties It is not possible to specify a duration greater than the maximum system allowed duration.
     */
    public String createToken( TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                               Map<String, Object> state, long duration ) throws Exception;

    /** Get the token info for the string version of this token */
    public TokenInfo getTokenInfo( String token ) throws Exception;

    /** Get the max token age in milliseconds */
    public long getMaxTokenAge( String token );


    /** Get the max token age in seconds */
    public long getMaxTokenAgeInSeconds( String token );

    /**
     * Expire the token.  If the token does not exist, this operation will not throw an error.  Implementations should
     * always delete this token, regardless of state.
     */
    public void revokeToken( String token );

    /**
     * Remove all tokens currently issued for the given AuthPrincipal.  Removes the specified type of token for the
     * given principal uuid and application uuid
     */
    public void removeTokens( AuthPrincipalInfo principal ) throws Exception;
}
