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


import java.nio.ByteBuffer;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.tokens.TokenCategory;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.security.tokens.exceptions.BadTokenException;
import org.apache.usergrid.security.tokens.exceptions.ExpiredTokenException;
import org.apache.usergrid.security.tokens.exceptions.InvalidTokenException;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import static java.lang.System.currentTimeMillis;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.codec.digest.DigestUtils.sha;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.getColumnMap;
import static org.apache.usergrid.persistence.cassandra.CassandraService.PRINCIPAL_TOKEN_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.TOKENS_CF;
import static org.apache.usergrid.security.tokens.TokenCategory.ACCESS;
import static org.apache.usergrid.security.tokens.TokenCategory.EMAIL;
import static org.apache.usergrid.security.tokens.TokenCategory.OFFLINE;
import static org.apache.usergrid.security.tokens.TokenCategory.REFRESH;
import static org.apache.usergrid.utils.ConversionUtils.HOLDER;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffer;
import static org.apache.usergrid.utils.ConversionUtils.bytes;
import static org.apache.usergrid.utils.ConversionUtils.getLong;
import static org.apache.usergrid.utils.ConversionUtils.string;
import static org.apache.usergrid.utils.ConversionUtils.uuid;
import static org.apache.usergrid.utils.MapUtils.hasKeys;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMillis;
import static org.apache.usergrid.persistence.cassandra.Serializers.*;


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
        return createToken( tokenCategory, type, principal, state, duration, System.currentTimeMillis() );
    }


    /** Exposed for testing purposes. The interface does not allow creation timestamp checking */
    public String createToken( TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                               Map<String, Object> state, long duration, long creationTimestamp ) throws Exception {

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
        TokenInfo tokenInfo = new TokenInfo( uuid, type, timestamp, timestamp, 0, duration, principal, state );
        putTokenInfo( tokenInfo );

        // generate token from the UUID that we created
        return getTokenForUUID(tokenInfo, tokenCategory, uuid);
    }


    @Override
    public void importToken(String token, TokenCategory tokenCategory, String type, AuthPrincipalInfo principal,
                            Map<String, Object> state, long duration) throws Exception {

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

        TokenInfo tokenInfo = new TokenInfo( uuid, type, timestamp, timestamp, 0, duration, principal, state );
        putTokenInfo( tokenInfo );
    }


    @Override
    public TokenInfo getTokenInfo( String token ) throws Exception {

        UUID uuid = getUUIDForToken( token );

        if ( uuid == null ) {
            return null;
        }

        TokenInfo tokenInfo = getTokenInfo( uuid );

        if ( tokenInfo == null ) {
            return null;
        }

        //update the token
        long now = currentTimeMillis();

        long maxTokenTtl = getMaxTtl( TokenCategory.getFromBase64String( token ), tokenInfo.getPrincipal() );

        Mutator<UUID> batch = createMutator( cassandra.getUsergridApplicationKeyspace(), ue );

        HColumn<String, Long> col =
                createColumn( TOKEN_ACCESSED, now, calcTokenTime( tokenInfo.getExpiration( maxTokenTtl ) ),
                        se, le );
        batch.addInsertion( uuid, TOKENS_CF, col );

        long inactive = now - tokenInfo.getAccessed();
        if ( inactive > tokenInfo.getInactive() ) {
            col = createColumn( TOKEN_INACTIVE, inactive, calcTokenTime( tokenInfo.getExpiration( maxTokenTtl ) ),
                    se, le );
            batch.addInsertion( uuid, TOKENS_CF, col );
            tokenInfo.setInactive( inactive );
        }

        batch.execute();

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
        return new TokenInfo( uuid, type, created, accessed, inactive, duration, principal, state );
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


    private UUID getUUIDForToken( String token ) throws ExpiredTokenException, BadTokenException {
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
}
