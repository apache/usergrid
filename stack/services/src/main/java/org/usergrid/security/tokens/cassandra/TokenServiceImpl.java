package org.usergrid.security.tokens.cassandra;

import static java.lang.System.currentTimeMillis;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.codec.digest.DigestUtils.sha;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.getColumnMap;
import static org.usergrid.persistence.cassandra.CassandraService.PRINCIPAL_TOKEN_CF;
import static org.usergrid.persistence.cassandra.CassandraService.TOKENS_CF;
import static org.usergrid.security.tokens.TokenCategory.ACCESS;
import static org.usergrid.security.tokens.TokenCategory.EMAIL;
import static org.usergrid.security.tokens.TokenCategory.OFFLINE;
import static org.usergrid.security.tokens.TokenCategory.REFRESH;
import static org.usergrid.utils.ConversionUtils.HOLDER;
import static org.usergrid.utils.ConversionUtils.bytebuffer;
import static org.usergrid.utils.ConversionUtils.bytes;
import static org.usergrid.utils.ConversionUtils.getLong;
import static org.usergrid.utils.ConversionUtils.string;
import static org.usergrid.utils.ConversionUtils.uuid;
import static org.usergrid.utils.MapUtils.hasKeys;
import static org.usergrid.utils.MapUtils.hashMap;
import static org.usergrid.utils.UUIDUtils.getTimestampInMillis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.Application;
import org.usergrid.security.AuthPrincipalInfo;
import org.usergrid.security.AuthPrincipalType;
import org.usergrid.security.tokens.TokenCategory;
import org.usergrid.security.tokens.TokenInfo;
import org.usergrid.security.tokens.TokenService;
import org.usergrid.security.tokens.exceptions.BadTokenException;
import org.usergrid.security.tokens.exceptions.ExpiredTokenException;
import org.usergrid.security.tokens.exceptions.InvalidTokenException;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.UUIDUtils;


public class TokenServiceImpl implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);
    
    public static final String PROPERTIES_AUTH_TOKEN_SECRET_SALT = "usergrid.auth.token_secret_salt";
    public static final String PROPERTIES_AUTH_TOKEN_EXPIRES_FROM_LAST_USE = "usergrid.auth.token_expires_from_last_use";
    public static final String PROPERTIES_AUTH_TOKEN_REFRESH_REUSES_ID = "usergrid.auth.token_refresh_reuses_id";

    private static final String TOKEN_UUID = "uuid";
    private static final String TOKEN_TYPE = "type";
    private static final String TOKEN_CREATED = "created";
    private static final String TOKEN_ACCESSED = "accessed";
    private static final String TOKEN_INACTIVE = "inactive";
    private static final String TOKEN_DURATION = "duration";
    private static final String TOKEN_PRINCIPAL_TYPE = "principal";
    private static final String TOKEN_ENTITY = "entity";
    private static final String TOKEN_APPLICATION = "application";
    private static final String TOKEN_STATE = "state";

    private static final String TOKEN_TYPE_ACCESS = "access";

    private static final ByteBufferSerializer BUFF_SER = ByteBufferSerializer.get();
    private static final StringSerializer STR_SER = StringSerializer.get();

    private static final HashSet<String> TOKEN_PROPERTIES = new HashSet<String>();

    static {
        TOKEN_PROPERTIES.add(TOKEN_UUID);
        TOKEN_PROPERTIES.add(TOKEN_TYPE);
        TOKEN_PROPERTIES.add(TOKEN_CREATED);
        TOKEN_PROPERTIES.add(TOKEN_ACCESSED);
        TOKEN_PROPERTIES.add(TOKEN_INACTIVE);
        TOKEN_PROPERTIES.add(TOKEN_PRINCIPAL_TYPE);
        TOKEN_PROPERTIES.add(TOKEN_ENTITY);
        TOKEN_PROPERTIES.add(TOKEN_APPLICATION);
        TOKEN_PROPERTIES.add(TOKEN_STATE);
        TOKEN_PROPERTIES.add(TOKEN_DURATION);
    }

    private static final HashSet<String> REQUIRED_TOKEN_PROPERTIES = new HashSet<String>();

    static {
        REQUIRED_TOKEN_PROPERTIES.add(TOKEN_UUID);
        REQUIRED_TOKEN_PROPERTIES.add(TOKEN_TYPE);
        REQUIRED_TOKEN_PROPERTIES.add(TOKEN_CREATED);
        REQUIRED_TOKEN_PROPERTIES.add(TOKEN_ACCESSED);
        REQUIRED_TOKEN_PROPERTIES.add(TOKEN_INACTIVE);
        REQUIRED_TOKEN_PROPERTIES.add(TOKEN_DURATION);
    }

    public static final String TOKEN_SECRET_SALT = "super secret token value";

    // Short-lived token is good for 24 hours
    public static final long SHORT_TOKEN_AGE = 24 * 60 * 60 * 1000;

    // Long-lived token is good for 7 days
    public static final long LONG_TOKEN_AGE = 7 * 24 * 60 * 60 * 1000;

    String tokenSecretSalt = TOKEN_SECRET_SALT;

    long maxPersistenceTokenAge = LONG_TOKEN_AGE;

    Map<TokenCategory, Long> tokenExpirations = hashMap(ACCESS, SHORT_TOKEN_AGE).map(REFRESH, LONG_TOKEN_AGE)
            .map(EMAIL, LONG_TOKEN_AGE).map(OFFLINE, LONG_TOKEN_AGE);

    long maxAccessTokenAge = SHORT_TOKEN_AGE;
    long maxRefreshTokenAge = LONG_TOKEN_AGE;
    long maxEmailTokenAge = LONG_TOKEN_AGE;
    long maxOfflineTokenAge = LONG_TOKEN_AGE;

    protected CassandraService cassandra;

    protected Properties properties;

    protected EntityManagerFactory emf;
    
    public TokenServiceImpl() {

    }

    long getExpirationProperty(String name, long default_expiration) {
        long expires = Long.parseLong(properties.getProperty("usergrid.auth.token." + name + ".expires", ""
                + default_expiration));
        return expires > 0 ? expires : default_expiration;
    }

    long getExpirationForTokenType(TokenCategory tokenCategory) {
        Long l = tokenExpirations.get(tokenCategory);
        if (l != null) {
            return l;
        }
        return SHORT_TOKEN_AGE;
    }

    void setExpirationFromProperties(String name) {
        TokenCategory tokenCategory = TokenCategory.valueOf(name.toUpperCase());
        long expires = Long.parseLong(properties.getProperty("usergrid.auth.token." + name + ".expires", ""
                + getExpirationForTokenType(tokenCategory)));
        if (expires > 0) {
            tokenExpirations.put(tokenCategory, expires);
        }
        logger.info("{} token expires after {} seconds", name,  getExpirationForTokenType(tokenCategory) / 1000 );
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;

        if (properties != null) {
            maxPersistenceTokenAge = getExpirationProperty("persistence", maxPersistenceTokenAge);

            setExpirationFromProperties("access");
            setExpirationFromProperties("refresh");
            setExpirationFromProperties("email");
            setExpirationFromProperties("offline");

            tokenSecretSalt = properties.getProperty(PROPERTIES_AUTH_TOKEN_SECRET_SALT, TOKEN_SECRET_SALT);
        }
    }

   

    @Override
    public String createToken(TokenCategory tokenCategory, String type, Map<String, Object> state) throws Exception {
        return createToken(tokenCategory, type, null, state);
    }

    @Override
    public String createToken(AuthPrincipalInfo principal) throws Exception {
        return createToken(TokenCategory.ACCESS, null, principal, null);
    }

    @Override
    public String createToken(AuthPrincipalInfo principal, Map<String, Object> state) throws Exception {
        return createToken(TokenCategory.ACCESS, null, principal, state);
    }
    
    

    /* (non-Javadoc)
     * @see org.usergrid.security.tokens.TokenService#createToken(org.usergrid.security.tokens.TokenCategory, java.lang.String, org.usergrid.security.AuthPrincipalInfo, java.util.Map)
     */
    @Override
    public String createToken(TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
            Map<String, Object> state) throws Exception {
        return createToken(tokenCategory, type, principal, state, 0);
    }

    @Override
    public String createToken(TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
            Map<String, Object> state, long duration) throws Exception {

        
        long maxTokenTtl = getMaxTtl(principal);
        
        if(duration > maxTokenTtl){
            throw new IllegalArgumentException(String.format("Your token age cannot be more than the maxium age of %d milliseconds", maxTokenTtl));
        }
        
        if(duration == 0){
            duration = maxTokenTtl;
        }
        
        if (principal != null) {
            Assert.notNull(principal.getType());
            Assert.notNull(principal.getApplicationId());
            Assert.notNull(principal.getUuid());
        }

        UUID uuid = UUIDUtils.newTimeUUID();
        long timestamp = getTimestampInMillis(uuid);
        if (type == null) {
            type = TOKEN_TYPE_ACCESS;
        }
        TokenInfo tokenInfo = new TokenInfo(uuid, type, timestamp, timestamp, 0, duration, principal, state);
        putTokenInfo(tokenInfo);
        return getTokenForUUID(tokenCategory, uuid);
    }

    @Override
    public TokenInfo getTokenInfo(String token) throws Exception {
        TokenInfo tokenInfo = null;
        UUID uuid = getUUIDForToken(token);
        if (uuid != null) {
            tokenInfo = getTokenInfo(uuid);
            if (tokenInfo != null) {
                long now = currentTimeMillis();
                
                long maxTokenTtl = getMaxTtl(tokenInfo.getPrincipal());

                Mutator<UUID> batch = createMutator(cassandra.getSystemKeyspace(), UUIDSerializer.get());

                HColumn<String, Long> col = createColumn(TOKEN_ACCESSED, now, (int) (tokenInfo.getExpiration(maxTokenTtl) / 1000),
                        StringSerializer.get(), LongSerializer.get());
                batch.addInsertion(uuid, TOKENS_CF, col);

                long inactive = now - tokenInfo.getAccessed();
                if (inactive > tokenInfo.getInactive()) {
                    col = createColumn(TOKEN_INACTIVE, inactive, (int) (tokenInfo.getExpiration(maxTokenTtl) / 1000),
                            StringSerializer.get(), LongSerializer.get());
                    batch.addInsertion(uuid, TOKENS_CF, col);
                    tokenInfo.setInactive(inactive);
                }

                batch.execute();
            }
        }
        return tokenInfo;
    }
    
    /**
     * Get the max ttl per app.  This is null safe,and will return the default in the case of missing data
     * @param principal
     * @return
     * @throws Exception
     */
    private long getMaxTtl(AuthPrincipalInfo principal) throws Exception{
        
        if(principal == null){
            return maxPersistenceTokenAge;
        }
        
        Application application = emf.getEntityManager(principal.getApplicationId()).getApplication();
        
        if(application == null){
            return maxPersistenceTokenAge;
        }
        
        //set the max to the default
        long maxTokenTtl = maxPersistenceTokenAge;
        
        //it's been defined on the expiration, override it
        if(application.getAccesstokenttl() != null){
            maxTokenTtl = application.getAccesstokenttl();
            
            //it's set to 0 which equals infinity, set our expiration to LONG.MAX
            if(maxTokenTtl == 0){
                maxTokenTtl = Long.MAX_VALUE;
            }
        }
        
        return maxTokenTtl;
    }

    @Override
    public String refreshToken(String token) throws Exception {
        TokenInfo tokenInfo = getTokenInfo(getUUIDForToken(token));
        if (tokenInfo != null) {
            putTokenInfo(tokenInfo);
            return getTokenForUUID(TokenCategory.ACCESS, tokenInfo.getUuid());
        }
        throw new InvalidTokenException("Token not found in database");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.security.tokens.TokenService#removeTokens(org.usergrid.security
     * .AuthPrincipalInfo)
     */
    @Override
    public void removeTokens(AuthPrincipalInfo principal) throws Exception {
        List<UUID> tokenIds = getTokenUUIDS(principal);

        Mutator<ByteBuffer> batch = createMutator(cassandra.getSystemKeyspace(), BUFF_SER);

        for (UUID tokenId : tokenIds) {
            batch.addDeletion(bytebuffer(tokenId), TOKENS_CF);
        }

        batch.addDeletion(principalKey(principal), PRINCIPAL_TOKEN_CF);

        batch.execute();

    }

    
    
    /* (non-Javadoc)
     * @see org.usergrid.security.tokens.TokenService#revokeToken(java.lang.String)
     */
    @Override
    public void revokeToken(String token) {
        
        
        TokenInfo info;
        
        try {
            info = getTokenInfo(token);
        } catch (Exception e) {
            logger.error("Unable to find token with the specified value ignoring request.  Value : {}", token);
            return;
        }
        
        UUID tokenId = info.getUuid();
        
      
        
        Mutator<ByteBuffer> batch = createMutator(cassandra.getSystemKeyspace(), BUFF_SER);

        //clean up the link in the principal -> token index if the principal is on the token
        if(info.getPrincipal() != null){
            batch.addDeletion(principalKey(info.getPrincipal()), PRINCIPAL_TOKEN_CF,   bytebuffer(tokenId), BUFF_SER );
        }
        
        //remove the token from the tokens cf
        batch.addDeletion(bytebuffer(tokenId), TOKENS_CF);
        


        batch.execute();
        
    }

    private TokenInfo getTokenInfo(UUID uuid) throws Exception {
        if (uuid == null) {
            throw new InvalidTokenException("No token specified");
        }
        Map<String, ByteBuffer> columns = getColumnMap(cassandra.getColumns(cassandra.getSystemKeyspace(), TOKENS_CF,
                uuid, TOKEN_PROPERTIES, StringSerializer.get(), ByteBufferSerializer.get()));
        if (!hasKeys(columns, REQUIRED_TOKEN_PROPERTIES)) {
            throw new InvalidTokenException("Token not found in database");
        }
        String type = string(columns.get(TOKEN_TYPE));
        long created = getLong(columns.get(TOKEN_CREATED));
        long accessed = getLong(columns.get(TOKEN_ACCESSED));
        long inactive = getLong(columns.get(TOKEN_INACTIVE));
        long duration = getLong(columns.get(TOKEN_DURATION));
        String principalTypeStr = string(columns.get(TOKEN_PRINCIPAL_TYPE));
        AuthPrincipalType principalType = null;
        if (principalTypeStr != null) {
            try {
                principalType = AuthPrincipalType.valueOf(principalTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        AuthPrincipalInfo principal = null;
        if (principalType != null) {
            UUID entityId = uuid(columns.get(TOKEN_ENTITY));
            UUID appId = uuid(columns.get(TOKEN_APPLICATION));
            principal = new AuthPrincipalInfo(principalType, entityId, appId);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) JsonUtils.fromByteBuffer(columns.get(TOKEN_STATE));
        return new TokenInfo(uuid, type, created, accessed, inactive, duration, principal, state);
    }

    private void putTokenInfo(TokenInfo tokenInfo) throws Exception {

        ByteBuffer tokenUUID = bytebuffer(tokenInfo.getUuid());

        Keyspace ko = cassandra.getSystemKeyspace();

        Mutator<ByteBuffer> m = createMutator(ko, BUFF_SER);

        int ttl = (int) (tokenInfo.getDuration() / 1000);

        m.addInsertion(tokenUUID, TOKENS_CF,
                createColumn(TOKEN_UUID, bytebuffer(tokenInfo.getUuid()), ttl, STR_SER, BUFF_SER));
        m.addInsertion(tokenUUID, TOKENS_CF,
                createColumn(TOKEN_TYPE, bytebuffer(tokenInfo.getType()), ttl, STR_SER, BUFF_SER));
        m.addInsertion(tokenUUID, TOKENS_CF,
                createColumn(TOKEN_CREATED, bytebuffer(tokenInfo.getCreated()), ttl, STR_SER, BUFF_SER));
        m.addInsertion(tokenUUID, TOKENS_CF,
                createColumn(TOKEN_ACCESSED, bytebuffer(tokenInfo.getAccessed()), ttl, STR_SER, BUFF_SER));
        m.addInsertion(tokenUUID, TOKENS_CF,
                createColumn(TOKEN_INACTIVE, bytebuffer(tokenInfo.getInactive()), ttl, STR_SER, BUFF_SER));
        m.addInsertion(tokenUUID, TOKENS_CF,
                createColumn(TOKEN_DURATION, bytebuffer(tokenInfo.getDuration()), ttl, STR_SER, BUFF_SER));

        if (tokenInfo.getPrincipal() != null) {

            AuthPrincipalInfo principalInfo = tokenInfo.getPrincipal();

            m.addInsertion(
                    tokenUUID,
                    TOKENS_CF,
                    createColumn(TOKEN_PRINCIPAL_TYPE, bytebuffer(principalInfo.getType().toString().toLowerCase()),
                            ttl, STR_SER, BUFF_SER));
            m.addInsertion(tokenUUID, TOKENS_CF,
                    createColumn(TOKEN_ENTITY, bytebuffer(principalInfo.getUuid()), ttl, STR_SER, BUFF_SER));
            m.addInsertion(
                    tokenUUID,
                    TOKENS_CF,
                    createColumn(TOKEN_APPLICATION, bytebuffer(principalInfo.getApplicationId()), ttl, STR_SER,
                            BUFF_SER));

            /*
             * write to the PRINCIPAL+TOKEN The format is as follow
             * 
             * appid+principalId+principalType :{ tokenuuid: 0x00}
             */

            ByteBuffer rowKey = principalKey(principalInfo);
            m.addInsertion(rowKey, PRINCIPAL_TOKEN_CF, createColumn(tokenUUID, HOLDER, ttl, BUFF_SER, BUFF_SER));
        }

        if (tokenInfo.getState() != null) {
            m.addInsertion(tokenUUID, TOKENS_CF,
                    createColumn(TOKEN_STATE, JsonUtils.toByteBuffer(tokenInfo.getState()), ttl, STR_SER, BUFF_SER));
        }

        m.execute();
    }

    /**
     * Load all the token uuids for a principal info
     * 
     * @param principal
     * @return
     * @throws Exception
     */
    private List<UUID> getTokenUUIDS(AuthPrincipalInfo principal) throws Exception {

        ByteBuffer rowKey = principalKey(principal);

        List<HColumn<ByteBuffer, ByteBuffer>> cols = cassandra.getColumns(cassandra.getSystemKeyspace(),
                PRINCIPAL_TOKEN_CF, rowKey, null, null, Integer.MAX_VALUE, false);

        List<UUID> results = new ArrayList<UUID>(cols.size());

        for (HColumn<ByteBuffer, ByteBuffer> col : cols) {
            results.add(uuid(col.getName()));
        }

        return results;
    }

    private ByteBuffer principalKey(AuthPrincipalInfo principalInfo) {
        // 66 bytes, 2 UUIDS + 2 chars for prefix
        ByteBuffer buff = ByteBuffer.allocate(32 * 2 + 2);
        buff.put(bytes(principalInfo.getApplicationId()));
        buff.put(bytes(principalInfo.getUuid()));
        buff.put(bytes(principalInfo.getType().getPrefix()));
        buff.rewind();

        return buff;

    }

    private UUID getUUIDForToken(String token) throws ExpiredTokenException, BadTokenException {
        TokenCategory tokenCategory = TokenCategory.getFromBase64String(token);
        byte[] bytes = decodeBase64(token.substring(TokenCategory.BASE64_PREFIX_LENGTH));
        UUID uuid = uuid(bytes);
        long timestamp = getTimestampInMillis(uuid);
        if ((getExpirationForTokenType(tokenCategory) > 0)
                && (currentTimeMillis() > (timestamp + getExpirationForTokenType(tokenCategory)))) {
            throw new ExpiredTokenException("Token expired "
                    + (currentTimeMillis() - (timestamp + getExpirationForTokenType(tokenCategory)))
                    + " millisecons ago.");
        }
        int i = 16;
        long expires = Long.MAX_VALUE;
        if (tokenCategory.getExpires()) {
            expires = ByteBuffer.wrap(bytes, i, 8).getLong();
            i = 24;
        }
        ByteBuffer expected = ByteBuffer.allocate(20);
        expected.put(sha(tokenCategory.getPrefix() + uuid + tokenSecretSalt + expires));
        expected.rewind();
        ByteBuffer signature = ByteBuffer.wrap(bytes, i, 20);
        if (!signature.equals(expected)) {
            throw new BadTokenException("Invalid token signature");
        }
        return uuid;
    }

    @Override
    public long getMaxTokenAge(String token) {
        TokenCategory tokenCategory = TokenCategory.getFromBase64String(token);
        byte[] bytes = decodeBase64(token.substring(TokenCategory.BASE64_PREFIX_LENGTH));
        UUID uuid = uuid(bytes);
        long timestamp = getTimestampInMillis(uuid);
        int i = 16;
        if (tokenCategory.getExpires()) {
            long expires = ByteBuffer.wrap(bytes, i, 8).getLong();
            return expires - timestamp;
        }
        return Long.MAX_VALUE;
    }

    /**
     * The maximum age a token can be saved for
     * @return the maxPersistenceTokenAge
     */
    public long getMaxPersistenceTokenAge() {
        return maxPersistenceTokenAge;
    }

    @Autowired
    @Qualifier("cassandraService")
    public void setCassandraService(CassandraService cassandra) {
        this.cassandra = cassandra;
    }
    
    @Autowired
    public void setEntityManagerFactory(EntityManagerFactory emf){
        this.emf = emf;
    }
    
    private String getTokenForUUID(TokenCategory tokenCategory, UUID uuid) {
        int l = 36;
        if (tokenCategory.getExpires()) {
            l += 8;
        }
        ByteBuffer bytes = ByteBuffer.allocate(l);
        bytes.put(bytes(uuid));
        long expires = Long.MAX_VALUE;
        if (tokenCategory.getExpires()) {
            expires = UUIDUtils.getTimestampInMillis(uuid) + getExpirationForTokenType(tokenCategory);
            bytes.putLong(expires);
        }
        bytes.put(sha(tokenCategory.getPrefix() + uuid + tokenSecretSalt + expires));
        return tokenCategory.getBase64Prefix() + encodeBase64URLSafeString(bytes.array());
    }

}
