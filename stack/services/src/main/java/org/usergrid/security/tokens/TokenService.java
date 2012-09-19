package org.usergrid.security.tokens;

import java.util.Map;

import org.usergrid.security.AuthPrincipalInfo;

public interface TokenService {

	public String createToken(TokenCategory tokenType, String type,
			Map<String, Object> state) throws Exception;

	public String createToken(AuthPrincipalInfo principal) throws Exception;

	public String createToken(AuthPrincipalInfo principal,
			Map<String, Object> state) throws Exception;

	public String createToken(TokenCategory tokenCategory, String type,
			AuthPrincipalInfo principal, Map<String, Object> state)
			throws Exception;

	/**
	 * Create the token with the given duration.  A duration value of 0 equals the default value specified in the properties
	 * It is not possible to specify a duration greater than the maximum system allowed duration.
	 */
	public String createToken(TokenCategory tokenCategory, String type,
            AuthPrincipalInfo principal, Map<String, Object> state, long duration)
            throws Exception;
	
	public TokenInfo getTokenInfo(String token) throws Exception;

	public String refreshToken(String token) throws Exception;

	public long getMaxTokenAge(String token);

	/**
	 * Remove all tokens currently issued for the given AuthPrincipal.  Removes the specified type of token
	 * for the given principal uuid and application uuid
	 * @param principal
	 * @throws Exception 
	 */
	public void removeTokens(AuthPrincipalInfo principal) throws Exception;
}
