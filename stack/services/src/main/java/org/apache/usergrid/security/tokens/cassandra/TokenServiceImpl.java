/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.security.tokens.cassandra;


import com.codahale.metrics.Counter;
import com.google.inject.Injector;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.exception.NotImplementedException;
import org.apache.usergrid.management.*;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.tokens.TokenCategory;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.security.tokens.exceptions.BadTokenException;
import org.apache.usergrid.security.tokens.exceptions.ExpiredTokenException;
import org.apache.usergrid.security.tokens.exceptions.InvalidTokenException;
import org.apache.usergrid.utils.ConversionUtils;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;
import org.codehaus.jackson.JsonNode;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.nio.ByteBuffer;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.codec.digest.DigestUtils.sha;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.getColumnMap;
import static org.apache.usergrid.persistence.cassandra.CassandraService.PRINCIPAL_TOKEN_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.TOKENS_CF;
import static org.apache.usergrid.persistence.cassandra.Serializers.*;
import static org.apache.usergrid.security.AuthPrincipalType.ADMIN_USER;
import static org.apache.usergrid.security.tokens.TokenCategory.*;
import static org.apache.usergrid.utils.ConversionUtils.*;
import static org.apache.usergrid.utils.MapUtils.hasKeys;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMillis;


public class TokenServiceImpl implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger( TokenServiceImpl.class );

    public static final String PROPERTIES_AUTH_TOKEN_SECRET_SALT = "usergrid.auth.token_secret_salt";
    public static final String PROPERTIES_AUTH_TOKEN_EXPIRES_FROM_LAST_USE =
            "usergrid.auth.token_expires_from_last_use";
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
    private static final String TOKEN_WORKFLOW_ORG_ID = "workflowOrgId";


    private static final String TOKEN_TYPE_ACCESS = "access";


    private static final Set<String> TOKEN_PROPERTIES;


    static {
        HashSet<String> set = new HashSet<String>();
        set.add( TOKEN_UUID );
        set.add( TOKEN_TYPE );
        set.add( TOKEN_CREATED );
        set.add( TOKEN_ACCESSED );
        set.add( TOKEN_INACTIVE );
        set.add( TOKEN_PRINCIPAL_TYPE );
        set.add( TOKEN_ENTITY );
        set.add( TOKEN_APPLICATION );
        set.add( TOKEN_STATE );
        set.add( TOKEN_DURATION );
        set.add( TOKEN_WORKFLOW_ORG_ID );
        TOKEN_PROPERTIES = Collections.unmodifiableSet(set);
    }


    private static final HashSet<String> REQUIRED_TOKEN_PROPERTIES = new HashSet<String>();


    static {
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_UUID );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_TYPE );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_CREATED );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_ACCESSED );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_INACTIVE );
        REQUIRED_TOKEN_PROPERTIES.add( TOKEN_DURATION );
    }


    public static final String TOKEN_SECRET_SALT = "super secret token value";

    // Short-lived token is good for 24 hours
    public static final long SHORT_TOKEN_AGE = 24 * 60 * 60 * 1000;

    // Long-lived token is good for 7 days
    public static final long LONG_TOKEN_AGE = 7 * 24 * 60 * 60 * 1000;

    String tokenSecretSalt = TOKEN_SECRET_SALT;

    long maxPersistenceTokenAge = LONG_TOKEN_AGE;

    Map<TokenCategory, Long> tokenExpirations =
            hashMap( ACCESS, LONG_TOKEN_AGE ).map( REFRESH, LONG_TOKEN_AGE ).map( EMAIL, LONG_TOKEN_AGE )
                    .map( OFFLINE, LONG_TOKEN_AGE );

    long maxAccessTokenAge = SHORT_TOKEN_AGE;
    long maxRefreshTokenAge = LONG_TOKEN_AGE;
    long maxEmailTokenAge = LONG_TOKEN_AGE;
    long maxOfflineTokenAge = LONG_TOKEN_AGE;

    protected CassandraService cassandra;

    protected Properties properties;

    protected EntityManagerFactory emf;

    protected MetricsFactory metricsFactory;


    public TokenServiceImpl() {
    }


    long getExpirationProperty( String name, long default_expiration ) {
        long expires = Long.parseLong(
                properties.getProperty( "usergrid.auth.token." + name + ".expires", "" + default_expiration ) );
        return expires > 0 ? expires : default_expiration;
    }


    long getExpirationForTokenType( TokenCategory tokenCategory ) {
        Long l = tokenExpirations.get( tokenCategory );
        if ( l != null ) {
            return l;
        }
        return SHORT_TOKEN_AGE;
    }


    void setExpirationFromProperties( String name ) {
        TokenCategory tokenCategory = TokenCategory.valueOf( name.toUpperCase() );
        long expires = Long.parseLong( properties.getProperty( "usergrid.auth.token." + name + ".expires",
                "" + getExpirationForTokenType( tokenCategory ) ) );
        if ( expires > 0 ) {
            tokenExpirations.put( tokenCategory, expires );
        }
        logger.info( "{} token expires after {} seconds", name, getExpirationForTokenType( tokenCategory ) / 1000 );
    }


    @Autowired
    public void setProperties( Properties properties ) {
        this.properties = properties;

        if ( properties != null ) {
            maxPersistenceTokenAge = getExpirationProperty( "persistence", maxPersistenceTokenAge );

            setExpirationFromProperties( "access" );
            setExpirationFromProperties( "refresh" );
            setExpirationFromProperties( "email" );
            setExpirationFromProperties( "offline" );

            tokenSecretSalt = properties.getProperty( PROPERTIES_AUTH_TOKEN_SECRET_SALT, TOKEN_SECRET_SALT );
        }
    }


    @Override
    public String createToken( TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                               Map<String, Object> state, long duration ) throws Exception {
        return createToken( tokenCategory, type, principal, state, duration, null, System.currentTimeMillis() );
    }


    @Override
    public String createToken( TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                               Map<String, Object> state, long duration, UUID workflowOrgId ) throws Exception {
        return createToken( tokenCategory, type, principal, state, duration, workflowOrgId, System.currentTimeMillis() );
    }


    /** Exposed for testing purposes. The interface does not allow creation timestamp checking */
    public String createToken( TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                               Map<String, Object> state, long duration, UUID workflowOrgId,
                               long creationTimestamp ) throws Exception {

        long maxTokenTtl = getMaxTtl( tokenCategory, principal );

        if ( duration > maxTokenTtl ) {
            throw new IllegalArgumentException(
                    String.format( "Your token age cannot be more than the maximum age of %d milliseconds",
                            maxTokenTtl ) );
        }

        if ( duration == 0 ) {
            duration = maxTokenTtl;
        }

        if ( principal != null ) {
            Assert.notNull( principal.getType() );
            Assert.notNull( principal.getApplicationId() );
            Assert.notNull( principal.getUuid() );
        }

        // create UUID that we will use to store token info in our database
        UUID uuid = UUIDUtils.newTimeUUID( creationTimestamp );

        long timestamp = getTimestampInMillis( uuid );
        if ( type == null ) {
            type = TOKEN_TYPE_ACCESS;
        }
        TokenInfo tokenInfo = new TokenInfo( uuid, type, timestamp, timestamp, 0, duration, principal,
                state, workflowOrgId );
        putTokenInfo( tokenInfo );

        // generate token from the UUID that we created
        return getTokenForUUID(tokenInfo, tokenCategory, uuid);
    }


    @Override
    public void importToken(String token, TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                            Map<String, Object> state, long duration) throws Exception {

        importToken(token, tokenCategory, type, principal, state, duration, null);
    }


    @Override
    public void importToken(String token, TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                            Map<String, Object> state, long duration, UUID workflowOrgId) throws Exception {

        // same logic as create token

        long maxTokenTtl = getMaxTtl( tokenCategory, principal );

        if ( duration > maxTokenTtl ) {
            throw new IllegalArgumentException(
                    String.format( "Your token age cannot be more than the maximum age of %d milliseconds",
                            maxTokenTtl ) );
        }

        if ( duration == 0 ) {
            duration = maxTokenTtl;
        }

        if ( principal != null ) {
            Assert.notNull( principal.getType() );
            Assert.notNull( principal.getApplicationId() );
            Assert.notNull( principal.getUuid() );
        }

        // except that we generate the UUID based on the token

        UUID uuid = getUUIDForToken(token);

        long timestamp = getTimestampInMillis( uuid );
        if ( type == null ) {
            type = TOKEN_TYPE_ACCESS;
        }

        TokenInfo tokenInfo = new TokenInfo( uuid, type, timestamp, timestamp, 0, duration, principal,
                state, workflowOrgId );
        putTokenInfo( tokenInfo );
    }


    @Override
    public TokenInfo getTokenInfo( String token ) throws Exception {
        return getTokenInfo(token, true);
    }


    @Override
    public TokenInfo getTokenInfo( String token, boolean updateAccessTime ) throws Exception {

        UUID uuid = getUUIDForToken( token );

        if ( uuid == null ) {
            return null;
        }

        TokenInfo tokenInfo;
        try {
            tokenInfo = getTokenInfo( uuid );
        } catch (InvalidTokenException e){
            // now try from central sso
            if ( isSSOEnabled() ){
                return validateExternalToken( token, maxPersistenceTokenAge );
            }else{
                throw e; // re-throw the error
            }
        }

        if (updateAccessTime) {
            //update the token
            long now = currentTimeMillis();

            long maxTokenTtl = getMaxTtl(TokenCategory.getFromBase64String(token), tokenInfo.getPrincipal());

            Mutator<UUID> batch = createMutator(cassandra.getUsergridApplicationKeyspace(), ue);

            HColumn<String, Long> col =
                    createColumn(TOKEN_ACCESSED, now, calcTokenTime(tokenInfo.getExpiration(maxTokenTtl)),
                            se, le);
            batch.addInsertion(uuid, TOKENS_CF, col);

            long inactive = now - tokenInfo.getAccessed();
            if (inactive > tokenInfo.getInactive()) {
                col = createColumn(TOKEN_INACTIVE, inactive, calcTokenTime(tokenInfo.getExpiration(maxTokenTtl)),
                        se, le);
                batch.addInsertion(uuid, TOKENS_CF, col);
                tokenInfo.setInactive(inactive);
            }

            batch.execute();
        }

        return tokenInfo;
    }


    /** Get the max ttl per app. This is null safe,and will return the default in the case of missing data */
    private long getMaxTtl( TokenCategory tokenCategory, AuthPrincipalInfo principal ) throws Exception {

        if ( principal == null ) {
            return maxPersistenceTokenAge;
        }
        long defaultMaxTtlForTokenType = getExpirationForTokenType( tokenCategory );

        Application application = emf.getEntityManager( principal.getApplicationId() )
                                     .get( principal.getApplicationId(), Application.class );

        if ( application == null ) {
            return defaultMaxTtlForTokenType;
        }

        // set the max to the default
        long maxTokenTtl = defaultMaxTtlForTokenType;

        // it's been defined on the expiration, override it
        if ( application.getAccesstokenttl() != null ) {
            maxTokenTtl = application.getAccesstokenttl();

            // it's set to 0 which equals infinity, set our expiration to
            // LONG.MAX
            if ( maxTokenTtl == 0 ) {
                maxTokenTtl = Long.MAX_VALUE;
            }
        }

        return maxTokenTtl;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.security.tokens.TokenService#removeTokens(org.apache.usergrid.security
     * .AuthPrincipalInfo)
     */
    @Override
    public void removeTokens( AuthPrincipalInfo principal ) throws Exception {
        List<UUID> tokenIds = getTokenUUIDS( principal );

        Mutator<ByteBuffer> batch = createMutator( cassandra.getUsergridApplicationKeyspace(), be );

        for ( UUID tokenId : tokenIds ) {
            batch.addDeletion( bytebuffer( tokenId ), TOKENS_CF );
        }

        batch.addDeletion( principalKey( principal ), PRINCIPAL_TOKEN_CF );

        batch.execute();
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.security.tokens.TokenService#revokeToken(java.lang.String)
     */
    @Override
    public void revokeToken( String token ) {

        TokenInfo info;

        try {
            info = getTokenInfo( token );
        }
        catch ( Exception e ) {
            logger.error( "Unable to find token with the specified value ignoring request.  Value : {}", token );
            return;
        }

        UUID tokenId = info.getUuid();

        Mutator<ByteBuffer> batch = createMutator( cassandra.getUsergridApplicationKeyspace(), be );

        // clean up the link in the principal -> token index if the principal is
        // on the token
        if ( info.getPrincipal() != null ) {
            batch.addDeletion( principalKey( info.getPrincipal() ), PRINCIPAL_TOKEN_CF, bytebuffer( tokenId ),
                    be );
        }

        // remove the token from the tokens cf
        batch.addDeletion( bytebuffer( tokenId ), TOKENS_CF );

        batch.execute();
    }


    private TokenInfo getTokenInfo( UUID uuid ) throws Exception {
        if ( uuid == null ) {
            throw new InvalidTokenException( "No token specified" );
        }
        Map<String, ByteBuffer> columns = getColumnMap( cassandra
                .getColumns( cassandra.getUsergridApplicationKeyspace(), TOKENS_CF, uuid, TOKEN_PROPERTIES, se,
                        be ) );
        if ( !hasKeys( columns, REQUIRED_TOKEN_PROPERTIES ) ) {
            throw new InvalidTokenException( "Token not found in database" );
        }
        String type = string( columns.get( TOKEN_TYPE ) );
        long created = getLong( columns.get( TOKEN_CREATED ) );
        long accessed = getLong( columns.get( TOKEN_ACCESSED ) );
        long inactive = getLong( columns.get( TOKEN_INACTIVE ) );
        long duration = getLong( columns.get( TOKEN_DURATION ) );
        String principalTypeStr = string( columns.get( TOKEN_PRINCIPAL_TYPE ) );
        AuthPrincipalType principalType = null;
        if ( principalTypeStr != null ) {
            try {
                principalType = AuthPrincipalType.valueOf( principalTypeStr.toUpperCase() );
            }
            catch ( IllegalArgumentException e ) {
            }
        }
        AuthPrincipalInfo principal = null;
        if ( principalType != null ) {
            UUID entityId = uuid( columns.get( TOKEN_ENTITY ) );
            UUID appId = uuid( columns.get( TOKEN_APPLICATION ) );
            principal = new AuthPrincipalInfo( principalType, entityId, appId );
        }
        @SuppressWarnings("unchecked") Map<String, Object> state =
                ( Map<String, Object> ) JsonUtils.fromByteBuffer( columns.get( TOKEN_STATE ) );

        UUID workflowOrgId = null;
        if (columns.containsKey(TOKEN_WORKFLOW_ORG_ID)) {
            workflowOrgId = ConversionUtils.uuid(columns.get(TOKEN_WORKFLOW_ORG_ID));
        }

        return new TokenInfo( uuid, type, created, accessed, inactive, duration, principal, state, workflowOrgId );
    }


    private void putTokenInfo( TokenInfo tokenInfo ) throws Exception {

        ByteBuffer tokenUUID = bytebuffer( tokenInfo.getUuid() );

        Keyspace ko = cassandra.getUsergridApplicationKeyspace();

        Mutator<ByteBuffer> m = createMutator( ko, be );

        int ttl = calcTokenTime( tokenInfo.getDuration() );

        m.addInsertion( tokenUUID, TOKENS_CF,
                createColumn( TOKEN_UUID, bytebuffer( tokenInfo.getUuid() ), ttl, se, be ) );
        m.addInsertion( tokenUUID, TOKENS_CF,
                createColumn( TOKEN_TYPE, bytebuffer( tokenInfo.getType() ), ttl, se, be ) );
        m.addInsertion( tokenUUID, TOKENS_CF,
                createColumn( TOKEN_CREATED, bytebuffer( tokenInfo.getCreated() ), ttl, se, be ) );
        m.addInsertion( tokenUUID, TOKENS_CF,
                createColumn( TOKEN_ACCESSED, bytebuffer( tokenInfo.getAccessed() ), ttl, se, be ) );
        m.addInsertion( tokenUUID, TOKENS_CF,
                createColumn( TOKEN_INACTIVE, bytebuffer( tokenInfo.getInactive() ), ttl, se, be ) );
        m.addInsertion( tokenUUID, TOKENS_CF,
                createColumn( TOKEN_DURATION, bytebuffer( tokenInfo.getDuration() ), ttl, se, be ) );

        if ( tokenInfo.getPrincipal() != null ) {

            AuthPrincipalInfo principalInfo = tokenInfo.getPrincipal();

            m.addInsertion( tokenUUID, TOKENS_CF,
                    createColumn( TOKEN_PRINCIPAL_TYPE, bytebuffer( principalInfo.getType().toString().toLowerCase() ),
                            ttl, se, be ) );
            m.addInsertion( tokenUUID, TOKENS_CF,
                    createColumn( TOKEN_ENTITY, bytebuffer( principalInfo.getUuid() ), ttl, se, be ) );
            m.addInsertion( tokenUUID, TOKENS_CF,
                    createColumn( TOKEN_APPLICATION, bytebuffer( principalInfo.getApplicationId() ), ttl, se,
                            be ) );

      /*
       * write to the PRINCIPAL+TOKEN The format is as follow
       *
       * appid+principalId+principalType :{ tokenuuid: 0x00}
       */

            ByteBuffer rowKey = principalKey( principalInfo );
            m.addInsertion( rowKey, PRINCIPAL_TOKEN_CF, createColumn( tokenUUID, HOLDER, ttl, be, be ) );
        }

        if ( tokenInfo.getState() != null ) {
            m.addInsertion( tokenUUID, TOKENS_CF,
                    createColumn( TOKEN_STATE, JsonUtils.toByteBuffer( tokenInfo.getState() ), ttl, se,
                            be ) );
        }

        if ( tokenInfo.getWorkflowOrgId() != null ) {
            m.addInsertion( tokenUUID, TOKENS_CF,
                    createColumn( TOKEN_WORKFLOW_ORG_ID, bytebuffer( tokenInfo.getWorkflowOrgId() ), ttl, se, be ) );
        }

        m.execute();
    }


    /** Load all the token uuids for a principal info */
    private List<UUID> getTokenUUIDS( AuthPrincipalInfo principal ) throws Exception {

        ByteBuffer rowKey = principalKey( principal );

        List<HColumn<ByteBuffer, ByteBuffer>> cols = cassandra
                .getColumns( cassandra.getUsergridApplicationKeyspace(), PRINCIPAL_TOKEN_CF, rowKey, null, null, Integer.MAX_VALUE,
                        false );

        List<UUID> results = new ArrayList<UUID>( cols.size() );

        for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
            results.add( uuid( col.getName() ) );
        }

        return results;
    }


    private ByteBuffer principalKey( AuthPrincipalInfo principalInfo ) {
        // 66 bytes, 2 UUIDS + 2 chars for prefix
        ByteBuffer buff = ByteBuffer.allocate( 32 * 2 + 2 );
        buff.put( bytes( principalInfo.getApplicationId() ) );
        buff.put( bytes( principalInfo.getUuid() ) );
        buff.put( bytes( principalInfo.getType().getPrefix() ) );
        buff.rewind();

        return buff;
    }


    private UUID getUUIDForToken(String token ) throws ExpiredTokenException, BadTokenException {
        TokenCategory tokenCategory = TokenCategory.getFromBase64String( token );
        byte[] bytes = decodeBase64( token.substring( TokenCategory.BASE64_PREFIX_LENGTH ) );
        UUID uuid = uuid( bytes );
        int i = 16;
        long expires = Long.MAX_VALUE;
        if ( tokenCategory.getExpires() ) {
            expires = ByteBuffer.wrap( bytes, i, 8 ).getLong();
            i = 24;
        }
        ByteBuffer expected = ByteBuffer.allocate( 20 );
        expected.put( sha( tokenCategory.getPrefix() + uuid + tokenSecretSalt + expires ) );
        expected.rewind();
        ByteBuffer signature = ByteBuffer.wrap( bytes, i, 20 );


        if ( !signature.equals( expected ) ) {
            throw new BadTokenException( "Invalid token signature" );
        }


        long expirationDelta = System.currentTimeMillis() - expires;

        if ( expires != Long.MAX_VALUE && expirationDelta > 0 ) {
            throw new ExpiredTokenException( String.format( "Token expired %d millisecons ago.", expirationDelta ) );
        }
        return uuid;
    }


    @Override
    public long getMaxTokenAge( String token ) {
        TokenCategory tokenCategory = TokenCategory.getFromBase64String( token );
        byte[] bytes = decodeBase64( token.substring( TokenCategory.BASE64_PREFIX_LENGTH ) );
        UUID uuid = uuid( bytes );
        long timestamp = getTimestampInMillis( uuid );
        int i = 16;
        if ( tokenCategory.getExpires() ) {
            long expires = ByteBuffer.wrap( bytes, i, 8 ).getLong();
            return expires - timestamp;
        }
        return Long.MAX_VALUE;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.security.tokens.TokenService#getMaxTokenAgeInSeconds(java.
     * lang.String)
     */
    @Override
    public long getMaxTokenAgeInSeconds( String token ) {
        return getMaxTokenAge( token ) / 1000;
    }


    /**
     * The maximum age a token can be saved for
     *
     * @return the maxPersistenceTokenAge
     */
    public long getMaxPersistenceTokenAge() {
        return maxPersistenceTokenAge;
    }


    @Autowired
    @Qualifier("cassandraService")
    public void setCassandraService( CassandraService cassandra ) {
        this.cassandra = cassandra;
    }


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
        final Injector injector = ((CpEntityManagerFactory)emf).getApplicationContext().getBean( Injector.class );
        metricsFactory = injector.getInstance(MetricsFactory.class);
    }


    private String getTokenForUUID( TokenInfo tokenInfo, TokenCategory tokenCategory, UUID uuid ) {
        int l = 36;
        if ( tokenCategory.getExpires() ) {
            l += 8;
        }
        ByteBuffer bytes = ByteBuffer.allocate( l );
        bytes.put( bytes( uuid ) );
        long expires = Long.MAX_VALUE;
        if ( tokenCategory.getExpires() ) {
            expires = ( tokenInfo.getDuration() > 0 ) ?
                      UUIDUtils.getTimestampInMillis( uuid ) + ( tokenInfo.getDuration() ) :
                      UUIDUtils.getTimestampInMillis( uuid ) + getExpirationForTokenType( tokenCategory );
            bytes.putLong( expires );
        }
        bytes.put( sha( tokenCategory.getPrefix() + uuid + tokenSecretSalt + expires ) );
        return tokenCategory.getBase64Prefix() + encodeBase64URLSafeString( bytes.array() );
    }


    /** Calculate the column lifetime and account for long truncation to seconds */
    private int calcTokenTime( long time ) {

        long secondsDuration = time / 1000;

        int ttl = ( int ) secondsDuration;

        // we've had a ttl that's longer than Integer.MAX value
        if ( ttl != secondsDuration ) {
            // Something is up with cassandra... Setting ttl to integer.max
            // makes the cols disappear.....

            // this should be the line below once this issue is fixed.
            // https://issues.apache.org/jira/browse/CASSANDRA-4771
            // ttl = Integer.MAX_VALUE

            // take the max value of an int, and substract the system time off
            // (in seconds) ,then arbitrarily remove another 120 seconds for good
            // measure.
            // Cass calcs the expiration time as
            // "(System.currentTimeMillis() / 1000) + timeToLive);", so we need
            // to play nice otherwise it blows up on persist
            ttl = Integer.MAX_VALUE - ( int ) ( System.currentTimeMillis() / 1000 ) - 120;
        }
        // hard cap at the max in o.a.c.db.IColumn
        if ( ttl > MAX_TTL ) {
            ttl = MAX_TTL;
        }

        return ttl;
    }


    private static final int MAX_TTL = 20 * 365 * 24 * 60 * 60;


    //-------------------------------------------------------------------------------------------------------
    //
    // Central SSO implementation

    public static final String USERGRID_CENTRAL_URL =         "usergrid.central.url";
    public static final String CENTRAL_CONNECTION_POOL_SIZE = "usergrid.central.connection.pool.size";
    public static final String CENTRAL_CONNECTION_TIMEOUT =   "usergrid.central.connection.timeout";
    public static final String CENTRAL_READ_TIMEOUT =         "usergrid.central.read.timeout";

    // names for metrics to be collected
    private static final String SSO_TOKENS_REJECTED =         "sso.tokens_rejected";
    private static final String SSO_TOKENS_VALIDATED =        "sso.tokens_validated";
    private static final String SSO_CREATED_LOCAL_ADMINS =    "sso.created_local_admins";
    private static final String SSO_PROCESSING_TIME =         "sso.processing_time";

    private static Client jerseyClient = null;

    @Autowired
    private ApplicationCreator applicationCreator;

    @Autowired
    protected ManagementService management;

    MetricsFactory getMetricsFactory() {
        return metricsFactory;
    }

    private boolean isSSOEnabled() {
        return !StringUtils.isEmpty( properties.getProperty( USERGRID_CENTRAL_URL ));
    }


    /**
     * <p>
     * Validates access token from other or "external" Usergrid system.
     * Calls other system's /management/me endpoint to get the User
     * associated with the access token. If user does not exist locally,
     * then user and organizations will be created. If no user is returned
     * from the other cluster, then return null.
     * </p>
     * <p/>
     * <p> Part of Usergrid Central SSO feature.
     * See <a href="https://issues.apache.org/jira/browse/USERGRID-567">USERGRID-567</a>
     * for details about Usergrid Central SSO.
     * </p>
     *
     * @param extAccessToken Access token from external Usergrid system.
     * @param ttl            Time to live for token.
     */
    public TokenInfo validateExternalToken(String extAccessToken, long ttl) throws Exception {

        TokenInfo tokenInfo = null;

        if (!isSSOEnabled()) {
            throw new NotImplementedException( "External Token Validation Service not enabled" );
        }

        if (extAccessToken == null) {
            throw new IllegalArgumentException( "ext_access_token must be specified" );
        }

        if (ttl == -1) {
            throw new IllegalArgumentException( "ttl must be specified" );
        }

        com.codahale.metrics.Timer processingTimer = getMetricsFactory().getTimer(
            TokenServiceImpl.class, SSO_PROCESSING_TIME );

        com.codahale.metrics.Timer.Context timerContext = processingTimer.time();

        try {
            // look up user via UG Central's /management/me endpoint.

            JsonNode accessInfoNode = getMeFromUgCentral( extAccessToken );

            JsonNode userNode = accessInfoNode.get( "user" );

            String username = userNode.get( "username" ).asText();

            // if user does not exist locally then we need to fix that

            UserInfo userInfo = management.getAdminUserByUsername( username );
            UUID userId = userInfo == null ? null : userInfo.getUuid();

            if (userId == null) {

                // create local user and and organizations they have on the central Usergrid instance
                logger.info( "User {} does not exist locally, creating", username );

                String name = userNode.get( "name" ).asText();
                String email = userNode.get( "email" ).asText();
                String dummyPassword = RandomStringUtils.randomAlphanumeric( 40 );

                JsonNode orgsNode = userNode.get( "organizations" );
                Iterator<String> fieldNames = orgsNode.getFieldNames();

                if (!fieldNames.hasNext()) {
                    // no organizations for user exist in response from central Usergrid SSO
                    // so create user's personal organization and use username as organization name
                    fieldNames = Collections.singletonList( username ).iterator();
                }

                // create user and any organizations that user is supposed to have

                while (fieldNames.hasNext()) {

                    String orgName = fieldNames.next();

                    if (userId == null) {

                        // haven't created user yet so do that now
                        OrganizationOwnerInfo ownerOrgInfo = management.createOwnerAndOrganization(
                            orgName, username, name, email, dummyPassword, true, false );

                        applicationCreator.createSampleFor( ownerOrgInfo.getOrganization() );

                        userId = ownerOrgInfo.getOwner().getUuid();
                        userInfo = ownerOrgInfo.getOwner();

                        Counter createdAdminsCounter = getMetricsFactory().getCounter(
                            TokenServiceImpl.class, SSO_CREATED_LOCAL_ADMINS );
                        createdAdminsCounter.inc();

                        logger.info( "Created user {} and org {}", username, orgName );

                    } else {

                        // already created user, so just create an org
                        final OrganizationInfo organization =
                            management.createOrganization( orgName, userInfo, true );

                        applicationCreator.createSampleFor( organization );

                        logger.info( "Created user {}'s other org {}", username, orgName );
                    }
                }
            }

            // store the external access_token as if it were one of our own
            importToken( extAccessToken, TokenCategory.ACCESS, null, new AuthPrincipalInfo(
                ADMIN_USER, userId, CpNamingUtils.MANAGEMENT_APPLICATION_ID), null, ttl );

            tokenInfo = getTokenInfo( extAccessToken );

        } catch (Exception e) {
            timerContext.stop();
            logger.debug( "Error validating external token", e );
            throw e;
        }

        return tokenInfo;
    }


    /**
     * Look up Admin User via UG Central's /management/me endpoint.
     *
     * @param extAccessToken Access token issued by UG Central of Admin User
     * @return JsonNode representation of AccessInfo object for Admin User
     * @throws EntityNotFoundException if access_token is not valid.
     */
    private JsonNode getMeFromUgCentral( String extAccessToken )  throws EntityNotFoundException {

        // prepare to count tokens validated and rejected

        Counter tokensRejectedCounter = getMetricsFactory().getCounter(
            TokenServiceImpl.class, SSO_TOKENS_REJECTED );
        Counter tokensValidatedCounter = getMetricsFactory().getCounter(
            TokenServiceImpl.class, SSO_TOKENS_VALIDATED );

        // create URL of central Usergrid's /management/me endpoint

        String externalUrl = properties.getProperty( USERGRID_CENTRAL_URL ).trim();

        // be lenient about trailing slash
        externalUrl = !externalUrl.endsWith( "/" ) ? externalUrl + "/" : externalUrl;
        String me = externalUrl + "management/me?access_token=" + extAccessToken;

        // use our favorite HTTP client to GET /management/me

        Client client = getJerseyClient();
        final JsonNode accessInfoNode;
        try {
            accessInfoNode = client.target( me ).request()
                .accept( MediaType.APPLICATION_JSON_TYPE )
                .get(JsonNode.class);

            tokensValidatedCounter.inc();

        } catch ( Exception e ) {
            // user not found 404
            tokensRejectedCounter.inc();
            String msg = "Cannot find Admin User associated with " + extAccessToken;
            throw new EntityNotFoundException( msg, e );
        }

        return accessInfoNode;
    }



    private Client getJerseyClient() {

        if ( jerseyClient == null ) {

            synchronized ( this ) {

                // create HTTPClient and with configured connection pool

                int poolSize = 100; // connections
                final String poolSizeStr = properties.getProperty( CENTRAL_CONNECTION_POOL_SIZE );
                if ( poolSizeStr != null ) {
                    poolSize = Integer.parseInt( poolSizeStr );
                }

                PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
                connectionManager.setMaxTotal(poolSize);

                int timeout = 20000; // ms
                final String timeoutStr = properties.getProperty( CENTRAL_CONNECTION_TIMEOUT );
                if ( timeoutStr != null ) {
                    timeout = Integer.parseInt( timeoutStr );
                }

                int readTimeout = 20000; // ms
                final String readTimeoutStr = properties.getProperty( CENTRAL_READ_TIMEOUT );
                if ( readTimeoutStr != null ) {
                    readTimeout = Integer.parseInt( readTimeoutStr );
                }

                ClientConfig clientConfig = new ClientConfig();
                clientConfig.register( new JacksonFeature() );
                clientConfig.property( ApacheClientProperties.CONNECTION_MANAGER, connectionManager );
                clientConfig.connectorProvider( new ApacheConnectorProvider() );

                jerseyClient = ClientBuilder.newClient( clientConfig );
                jerseyClient.property( ClientProperties.CONNECT_TIMEOUT, timeout );
                jerseyClient.property( ClientProperties.READ_TIMEOUT, readTimeout );
            }
        }

        return jerseyClient;

    }


}
