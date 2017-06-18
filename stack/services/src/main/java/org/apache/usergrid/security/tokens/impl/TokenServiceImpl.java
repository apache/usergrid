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
package org.apache.usergrid.security.tokens.impl;


import com.google.inject.Injector;
import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.token.TokenSerialization;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.sso.SSOProviderFactory;
import org.apache.usergrid.security.tokens.TokenCategory;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.security.tokens.exceptions.BadTokenException;
import org.apache.usergrid.security.tokens.exceptions.ExpiredTokenException;
import org.apache.usergrid.security.tokens.exceptions.InvalidTokenException;
import org.apache.usergrid.security.sso.ExternalSSOProvider;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.ws.rs.client.Client;
import java.nio.ByteBuffer;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.codec.digest.DigestUtils.sha;
import static org.apache.usergrid.persistence.token.impl.TokenSerializationImpl.*;
import static org.apache.usergrid.security.AuthPrincipalType.ADMIN_USER;
import static org.apache.usergrid.security.tokens.TokenCategory.*;
import static org.apache.usergrid.utils.ConversionUtils.*;
import static org.apache.usergrid.utils.MapUtils.hasKeys;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMillis;


public class TokenServiceImpl implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger( TokenServiceImpl.class );

    public static final String PROPERTIES_AUTH_TOKEN_SECRET_SALT = "usergrid.auth.token_secret_salt";

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

    protected Properties properties;

    protected EntityManagerFactory emf;

    private MetricsFactory metricsFactory;

    private TokenSerialization tokenSerialization;


    public TokenServiceImpl() {
    }


    private long getExpirationProperty( String name, long default_expiration ) {
        long expires = Long.parseLong(
                properties.getProperty( "usergrid.auth.token." + name + ".expires", "" + default_expiration ) );
        return expires > 0 ? expires : default_expiration;
    }


    private long getExpirationForTokenType( TokenCategory tokenCategory ) {
        Long l = tokenExpirations.get( tokenCategory );
        if ( l != null ) {
            return l;
        }
        return SHORT_TOKEN_AGE;
    }


    private void setExpirationFromProperties( String name ) {
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

        UUID uuid;


        /** Pre-validation of the token string based on Usergrid's encoding scheme.
         *
         * If the token does not parse out a UUID, then it's not a Usergrid token.  Check if External SSO provider
         * is configured, which is not Usergrid and immediately try to validate the token based on this parsing
         * information.
         */
        try{
            uuid = getUUIDForToken( token );
        }
        catch (ExpiredTokenException expiredTokenException){
            throw new ExpiredTokenException(expiredTokenException.getMessage());
        }
        catch(Exception e){

            // If the token doesn't parse as a Usergrid token, see if an external provider other than Usergrid is
            // enabled.  If so, just validate the external token.
            try{
                if( isExternalSSOProviderEnabled() && !getExternalSSOProvider().equalsIgnoreCase("usergrid")) {
                    return validateExternalToken(token, 1, getExternalSSOProvider());
                }else{
                    throw new IllegalArgumentException("invalid external provider : " + getExternalSSOProvider()); // re-throw the error
                }
            }
            catch (NullPointerException npe){
                throw new IllegalArgumentException("The SSO provider in the config is empty.");
            }

        }

        final TokenInfo tokenInfo;

        /**
         * Now try actual Usergrid token validations.  First try locally.  If that fails and SSO is enabled with
         * Usergrid being a provider, validate the external token.
         */
        try {
            tokenInfo = getTokenInfo( uuid );
        } catch (InvalidTokenException e){
            // Try the request from Usergrid, conditions are specific so we don't incur perf hits for unncessary
            // token validations that are known to not
            if ( isExternalSSOProviderEnabled() && getExternalSSOProvider().equalsIgnoreCase("usergrid") ){
                return validateExternalToken( token, maxPersistenceTokenAge, getExternalSSOProvider() );
            }else{
                throw e; // re-throw the error
            }
        }

        if (updateAccessTime) {
            //update the token
            long now = currentTimeMillis();

            long maxTokenTtl = getMaxTtl(TokenCategory.getFromBase64String(token), tokenInfo.getPrincipal());

            long inactive = now - tokenInfo.getAccessed();
            // Long.MIN_VALUE indicates that nothing needs to be updated for token inactive property
            if (inactive < tokenInfo.getInactive()) {
               inactive = Long.MIN_VALUE;
            }

            tokenSerialization.updateTokenAccessTime(uuid, now, inactive, calcTokenTime(tokenInfo.getExpiration(maxTokenTtl)));
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

        final List<UUID> tokenIds = getTokenUUIDS( principal );
        tokenSerialization.deleteTokens(tokenIds, principalKey( principal ));

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

        final UUID tokenId = info.getUuid();

        // clean up the link in the principal -> token index if the principal is
        // on the token
        if ( info.getPrincipal() != null ) {
            tokenSerialization.revokeToken(tokenId, principalKey( info.getPrincipal()));
        }else{
            tokenSerialization.revokeToken(tokenId, null);
        }

    }


    private TokenInfo getTokenInfo( UUID uuid ) throws Exception {
        if ( uuid == null ) {
            throw new InvalidTokenException( "No token specified" );
        }

        Map<String, Object> tokenDetails = tokenSerialization.getTokenInfo(uuid);

        if ( !hasKeys( tokenDetails, REQUIRED_TOKEN_PROPERTIES ) ) {
            throw new InvalidTokenException( "Token not found in database" );
        }

        String type = (String) tokenDetails.get(TOKEN_TYPE);
        long created = (long) tokenDetails.get(TOKEN_CREATED);
        long accessed = (long) tokenDetails.get(TOKEN_ACCESSED);
        long inactive = (long) tokenDetails.get(TOKEN_INACTIVE);
        long duration = (long) tokenDetails.get(TOKEN_DURATION);

        String principalTypeStr = (String) tokenDetails.get(TOKEN_PRINCIPAL_TYPE);

        AuthPrincipalType principalType = null;
        if ( principalTypeStr != null ) {
            try {
                principalType = AuthPrincipalType.valueOf( principalTypeStr.toUpperCase() );
            }
            catch ( IllegalArgumentException e ) {
                logger.warn("Unable to convert authPrincipal Type from string to enum");
            }
        }
        AuthPrincipalInfo principal = null;
        if ( principalType != null ) {
            UUID entityId = (UUID) tokenDetails.get(TOKEN_ENTITY);
            UUID appId = (UUID) tokenDetails.get(TOKEN_APPLICATION);
            principal = new AuthPrincipalInfo( principalType, entityId, appId );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> state = ( Map<String, Object> ) tokenDetails.get( TOKEN_STATE );

        UUID workflowOrgId = null;
        if (tokenDetails.containsKey(TOKEN_WORKFLOW_ORG_ID)) {
            workflowOrgId = (UUID) tokenDetails.get(TOKEN_WORKFLOW_ORG_ID);
        }

        return new TokenInfo( uuid, type, created, accessed, inactive, duration, principal, state, workflowOrgId );
    }


    private void putTokenInfo( TokenInfo tokenInfo ) throws Exception {

        int ttl = calcTokenTime( tokenInfo.getDuration() );
        final Map<String, Object> tokenDetails = new HashMap<>();

        tokenDetails.put(TOKEN_UUID, tokenInfo.getUuid());
        tokenDetails.put(TOKEN_TYPE, tokenInfo.getType());
        tokenDetails.put(TOKEN_CREATED, tokenInfo.getCreated());
        tokenDetails.put(TOKEN_ACCESSED, tokenInfo.getAccessed());
        tokenDetails.put(TOKEN_INACTIVE, tokenInfo.getInactive());
        tokenDetails.put(TOKEN_DURATION, tokenInfo.getDuration());

        ByteBuffer principalKeyBuffer = null;
        if ( tokenInfo.getPrincipal() != null ) {

            AuthPrincipalInfo principalInfo = tokenInfo.getPrincipal();

            tokenDetails.put(TOKEN_PRINCIPAL_TYPE, principalInfo.getType().toString().toLowerCase());
            tokenDetails.put(TOKEN_ENTITY, principalInfo.getUuid());
            tokenDetails.put(TOKEN_APPLICATION, principalInfo.getApplicationId());

          /*
           * write to the PRINCIPAL+TOKEN The format is as follow
           *
           * appid+principalId+principalType :{ tokenuuid: 0x00}
           */
            principalKeyBuffer = principalKey( principalInfo );

        }

        if ( tokenInfo.getState() != null ) {
            tokenDetails.put(TOKEN_STATE, tokenInfo.getState());
        }

        if ( tokenInfo.getWorkflowOrgId() != null ) {
            tokenDetails.put(TOKEN_WORKFLOW_ORG_ID, tokenInfo.getWorkflowOrgId());
        }

        tokenSerialization.putTokenInfo(tokenInfo.getUuid(), tokenDetails, principalKeyBuffer, ttl);
    }


    /** Load all the token uuids for a principal info */
    private List<UUID> getTokenUUIDS( AuthPrincipalInfo principal ) throws Exception {

        return tokenSerialization.getTokensForPrincipal(principalKey( principal ));
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
            throw new ExpiredTokenException( String.format( "Token expired %d milliseconds ago.", expirationDelta ) );
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
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
        final Injector injector = ((CpEntityManagerFactory)emf).getApplicationContext().getBean( Injector.class );
        this.metricsFactory = injector.getInstance(MetricsFactory.class);
        this.tokenSerialization = injector.getInstance(TokenSerialization.class);
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

    public static final String CENTRAL_CONNECTION_POOL_SIZE = "usergrid.central.connection.pool.size";
    public static final String CENTRAL_CONNECTION_TIMEOUT =   "usergrid.central.connection.timeout";
    public static final String CENTRAL_READ_TIMEOUT =         "usergrid.central.read.timeout";


    // names for metrics to be collected
    private static final String SSO_TOKENS_REJECTED =         "sso.tokens_rejected";
    private static final String SSO_TOKENS_VALIDATED =        "sso.tokens_validated";
    private static final String SSO_CREATED_LOCAL_ADMINS =    "sso.created_local_admins";
    private static final String SSO_PROCESSING_TIME =         "sso.processing_time";

    //SSO2 implementation
    public static final String USERGRID_EXTERNAL_SSO_ENABLED = "usergrid.external.sso.enabled";
    public static final String USERGRID_EXTERNAL_SSO_PROVIDER =    "usergrid.external.sso.provider";
    public static final String USERGRID_EXTERNAL_SSO_PROVIDER_URL = "usergrid.external.sso.url";
    public static final String USERGRID_EXTERNAL_SSO_PROVIDER_USER_PROVISION_URL
        = "usergrid.external.sso.userprovision.url";


    private static Client jerseyClient = null;

    @Autowired
    private ApplicationCreator applicationCreator;

    @Autowired
    protected ManagementService management;

    @Autowired
    private SSOProviderFactory ssoProviderFactory;

    MetricsFactory getMetricsFactory() {
        return metricsFactory;
    }


    public boolean isExternalSSOProviderEnabled() {
        return Boolean.valueOf(properties.getProperty( USERGRID_EXTERNAL_SSO_ENABLED ));
    }

    private String getExternalSSOProvider(){
            return properties.getProperty(USERGRID_EXTERNAL_SSO_PROVIDER);
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
    public TokenInfo validateExternalToken(String extAccessToken, long ttl, String provider) throws Exception {


        ExternalSSOProvider ssoProvider = ssoProviderFactory.getProvider();

        if(provider.equalsIgnoreCase("usergrid")){

            UserInfo userinfo = ssoProvider.validateAndReturnUserInfo(extAccessToken,ttl);

            // Store the external Usergrid access_token as if it were one of our own so we don't have to make the
            // external HTTP validation call on subsequent requests
            importToken( extAccessToken, TokenCategory.ACCESS, null, new AuthPrincipalInfo(
                ADMIN_USER, userinfo.getUuid(), CpNamingUtils.MANAGEMENT_APPLICATION_ID), null, ttl );
            return getTokenInfo( extAccessToken );

        }else{

            return ssoProvider.validateAndReturnTokenInfo(extAccessToken,ttl);
        }

    }

}
