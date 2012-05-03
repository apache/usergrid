package org.usergrid.security.tokens;

import java.util.Map;

import org.usergrid.security.AccessTokenInfo;
import org.usergrid.security.AuthPrincipalInfo;

public interface AccessTokenManager {

	public String createAccessToken(String type, Map<String, Object> state)
			throws Exception;

	public String createAccessToken(AuthPrincipalInfo principal,
			Map<String, Object> state) throws Exception;

	public AccessTokenInfo getAccessTokenInfo(String token) throws Exception;

	public String refreshAccessToken(String token) throws Exception;

	public long getMaxTokenAge();
}
