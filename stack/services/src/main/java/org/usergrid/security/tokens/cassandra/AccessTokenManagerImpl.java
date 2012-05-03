package org.usergrid.security.tokens.cassandra;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.codec.digest.DigestUtils.sha;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.getColumnMap;
import static org.usergrid.persistence.cassandra.CassandraService.TOKENS_CF;
import static org.usergrid.utils.ConversionUtils.bytebuffer;
import static org.usergrid.utils.ConversionUtils.bytes;
import static org.usergrid.utils.ConversionUtils.getLong;
import static org.usergrid.utils.ConversionUtils.string;
import static org.usergrid.utils.ConversionUtils.uuid;
import static org.usergrid.utils.MapUtils.hasKeys;
import static org.usergrid.utils.UUIDUtils.getTimestampInMillis;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.security.AccessTokenInfo;
import org.usergrid.security.AuthPrincipalInfo;
import org.usergrid.security.AuthPrincipalType;
import org.usergrid.security.tokens.AccessTokenManager;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.UUIDUtils;

public class AccessTokenManagerImpl implements AccessTokenManager {

	private static final String TOKEN_STATE = "state";
	private static final String TOKEN_APPLICATION = "application";
	private static final String TOKEN_ENTITY = "entity";
	private static final String TOKEN_ACCESSED = "accessed";
	private static final String TOKEN_CREATED = "created";
	private static final String TOKEN_TYPE = "type";
	private static final String TOKEN_UUID = "uuid";

	public static final String TOKEN_SECRET_SALT = "super secret token value";

	// Token is good for 24 hours
	public static final long MAX_TOKEN_AGE = 24 * 60 * 60 * 1000;

	String tokenSecretSalt = TOKEN_SECRET_SALT;

	long maxTokenAge = MAX_TOKEN_AGE;

	protected CassandraService cassandra;

	protected Properties properties;

	public AccessTokenManagerImpl() {

	}

	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;

		if (properties != null) {
			maxTokenAge = Long.parseLong(properties.getProperty(
					"usergrid.auth.token_max_age", "" + MAX_TOKEN_AGE));
			maxTokenAge = maxTokenAge > 0 ? maxTokenAge : MAX_TOKEN_AGE;

			tokenSecretSalt = properties.getProperty(
					"usergrid.auth.token_secret_salt", TOKEN_SECRET_SALT);
		}
	}

	public void setCassandraService(CassandraService cassandra) {
		this.cassandra = cassandra;
	}

	@Override
	public long getMaxTokenAge() {
		return maxTokenAge;
	}

	@Override
	public String createAccessToken(String type, Map<String, Object> state)
			throws Exception {
		UUID uuid = UUIDUtils.newTimeUUID();
		long timestamp = getTimestampInMillis(uuid);
		AccessTokenInfo tokenInfo = new AccessTokenInfo(uuid, type, timestamp,
				timestamp, null, state);
		putAccessTokenInfo(tokenInfo);
		return getTokenForUUID(uuid);
	}

	@Override
	public String createAccessToken(AuthPrincipalInfo principal,
			Map<String, Object> state) throws Exception {
		UUID uuid = UUIDUtils.newTimeUUID();
		long timestamp = getTimestampInMillis(uuid);
		AccessTokenInfo tokenInfo = new AccessTokenInfo(uuid, principal
				.getType().toString().toLowerCase(), timestamp, timestamp,
				principal, state);
		putAccessTokenInfo(tokenInfo);
		return getTokenForUUID(uuid);
	}

	@Override
	public AccessTokenInfo getAccessTokenInfo(String token) throws Exception {
		// TODO if tokens auto refresh, do so here
		return getAccessTokenInfo(getUUIDForToken(token));
	}

	@Override
	public String refreshAccessToken(String token) throws Exception {
		getAccessTokenInfo(getUUIDForToken(token));

		return null;
	}

	public AccessTokenInfo getAccessTokenInfo(UUID uuid) throws Exception {
		Map<String, ByteBuffer> columns = getColumnMap(cassandra.getAllColumns(
				cassandra.getSystemKeyspace(), TOKENS_CF, uuid));
		if (!hasKeys(columns, TOKEN_UUID, TOKEN_TYPE, TOKEN_CREATED,
				TOKEN_ACCESSED)) {
			return null;
		}
		String type = string(columns.get(TOKEN_TYPE));
		long created = getLong(columns.get(TOKEN_CREATED));
		long accessed = getLong(columns.get(TOKEN_ACCESSED));
		AuthPrincipalType principalType = AuthPrincipalType.valueOf(type
				.toUpperCase());
		AuthPrincipalInfo principal = null;
		if (principalType != null) {
			UUID entityId = uuid(columns.get(TOKEN_ENTITY));
			UUID appId = uuid(columns.get(TOKEN_APPLICATION));
			principal = new AuthPrincipalInfo(principalType, entityId, appId);
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> state = (Map<String, Object>) JsonUtils
				.fromByteBuffer(columns.get(TOKEN_STATE));
		return new AccessTokenInfo(uuid, type, created, accessed, principal,
				state);
	}

	public void putAccessTokenInfo(AccessTokenInfo tokenInfo) throws Exception {
		Map<String, ByteBuffer> columns = new HashMap<String, ByteBuffer>();
		columns.put(TOKEN_UUID, bytebuffer(tokenInfo.getUuid()));
		columns.put(TOKEN_TYPE, bytebuffer(tokenInfo.getType()));
		columns.put(TOKEN_CREATED, bytebuffer(tokenInfo.getCreated()));
		columns.put(TOKEN_ACCESSED, bytebuffer(tokenInfo.getAccessed()));
		if (tokenInfo.getPrincipal() != null) {
			columns.put(TOKEN_ENTITY, bytebuffer(tokenInfo.getPrincipal()
					.getUuid()));
			columns.put(TOKEN_APPLICATION, bytebuffer(tokenInfo.getPrincipal()
					.getApplicationId()));
		}
		cassandra.setColumns(cassandra.getSystemKeyspace(), TOKENS_CF,
				bytes(tokenInfo.getUuid()), columns, (int) maxTokenAge);
	}

	public UUID getUUIDForToken(String token) {
		byte[] bytes = decodeBase64(token);
		UUID uuid = uuid(bytes);
		long timestamp = getTimestampInMillis(uuid);
		if ((maxTokenAge > 0)
				&& (System.currentTimeMillis() > (timestamp + maxTokenAge))) {
			return null;
		}
		ByteBuffer expected = ByteBuffer.allocate(20);
		expected.put(sha(uuid + tokenSecretSalt));
		ByteBuffer signature = ByteBuffer.wrap(bytes, 16, 20);
		if (!signature.equals(expected)) {
			return null;
		}
		return uuid;
	}

	public String getTokenForUUID(UUID uuid) {
		ByteBuffer bytes = ByteBuffer.allocate(36);
		bytes.put(bytes(uuid));
		bytes.put(sha(uuid + tokenSecretSalt));
		return encodeBase64URLSafeString(bytes.array());
	}

}
