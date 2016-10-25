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
package org.apache.usergrid.security.sso;

import io.jsonwebtoken.*;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.exceptions.ExternalSSOProviderAdminUserNotFoundException;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.tokens.TokenInfo;
import org.apache.usergrid.security.tokens.exceptions.BadTokenException;
import org.apache.usergrid.security.tokens.exceptions.ExpiredTokenException;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.commons.codec.binary.Base64.decodeBase64;


public class ApigeeSSO2Provider implements ExternalSSOProvider {

    private static final Logger logger = LoggerFactory.getLogger(ApigeeSSO2Provider.class);
    private static final String RESPONSE_PUBLICKEY_VALUE = "value";
    protected Properties properties;
    protected ManagementService management;
    protected Client client;
    protected PublicKey publicKey;
    protected long freshnessTime = 3000L;

    public long lastPublicKeyFetch = 0L;


    public static final String USERGRID_EXTERNAL_PUBLICKEY_URL = "usergrid.external.sso.url";

    public static final String USERGRID_EXTERMAL_PUBLICKEY_FRESHNESS = "usergrid.external.sso.public-key-freshness";


    public ApigeeSSO2Provider() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JacksonFeature());
        client = ClientBuilder.newClient(clientConfig);
    }

    public PublicKey getPublicKey(String keyUrl) {

        if ( keyUrl != null && !keyUrl.isEmpty()) {
            try {
                Map<String, Object> publicKey = client.target(keyUrl).request().get(Map.class);
                String ssoPublicKey = publicKey.get(RESPONSE_PUBLICKEY_VALUE)
                    .toString().split("----\n")[1].split("\n---")[0];
                byte[] publicBytes = decodeBase64(ssoPublicKey);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey pubKey = keyFactory.generatePublic(keySpec);
                return pubKey;
            }
            catch (Exception e) {
                throw new IllegalArgumentException("error getting public key");
            }
        }

        return null;
    }

    @Override
    public TokenInfo validateAndReturnTokenInfo(String token, long ttl) throws Exception {


        UserInfo userInfo = validateAndReturnUserInfo(token, ttl);

        if (userInfo == null) {
            throw new ExternalSSOProviderAdminUserNotFoundException("Unable to load user from token: " + token);
        }

        return new TokenInfo(UUIDUtils.newTimeUUID(), "access", 1, 1, 1, ttl,
                new AuthPrincipalInfo(AuthPrincipalType.ADMIN_USER, userInfo.getUuid(),
                    CpNamingUtils.MANAGEMENT_APPLICATION_ID), null);
    }

    @Override
    public UserInfo validateAndReturnUserInfo(String token, long ttl) throws Exception {

        Jws<Claims> payload = getClaims(token);

        // this step super important to ensure the token is a valid token
        validateClaims(payload);

        UserInfo userInfo = management.getAdminUserByEmail(payload.getBody().get("email").toString());

        return userInfo;
    }

    @Override
    public Map<String, String> getDecodedTokenDetails(String token) throws Exception {

       Jws<Claims> jws = getClaims(token);

        Claims claims = jws.getBody();
        Map<String, String> tokenDetails = new HashMap<>();

        tokenDetails.put("username", (String)claims.get("user_name"));
        tokenDetails.put("email", (String)claims.get("email"));
        tokenDetails.put("expiry", claims.get("exp").toString());
        tokenDetails.put("user_id", claims.get("user_id").toString());


        return tokenDetails;

    }

    @Override
    public Map<String, Object> getAllTokenDetails(String token, String keyUrl) throws Exception {
        Jws<Claims> claims = getClaimsForKeyUrl( token );
        return JsonUtils.toJsonMap(claims.getBody());

    }

    @Override
    public String getExternalSSOUrl() {
        return properties.getProperty(USERGRID_EXTERNAL_PUBLICKEY_URL);
    }

    public Jws<Claims> getClaimsForKeyUrl( String token ) throws BadTokenException {

        Jws<Claims> claims = null;

        Exception lastException = null;

        int tries = 0;
        int maxTries = 2;
        while ( claims == null && tries++ < maxTries ) {
            try {
                claims = Jwts.parser().setSigningKey( publicKey ).parseClaimsJws( token );

            } catch (SignatureException se) {
                // bad signature, need to get latest publicKey and try again
                // logger.debug( "Signature was invalid for Apigee JWT token: {}", token );
                lastException = se;

            } catch (ArrayIndexOutOfBoundsException aio) {
                // unknown error, need to get latest publicKey and try again
                logger.debug("Error parsing JWT token", aio);
                throw new BadTokenException( "Unknown error processing JWT", aio );

            } catch (ExpiredJwtException e) {
                final long expiry = Long.valueOf( e.getClaims().get( "exp" ).toString() );
                final long expirationDelta = ((System.currentTimeMillis() / 1000) - expiry) * 1000;
                logger.debug(String.format("Apigee JWT Token expired %d milliseconds ago.", expirationDelta));

                // token is expired
                throw new BadTokenException( "Expired JWT", e );

            } catch (MalformedJwtException me) {
                logger.debug( "Malformed JWT", me );

                // token is malformed
                throw new BadTokenException( "Malformed JWT", me );
            }

            long keyFreshness = System.currentTimeMillis() - lastPublicKeyFetch;
            if ( claims == null && keyFreshness > this.freshnessTime ) {
                logger.debug("Failed to get claims for token {}... fetching new public key", token);
                publicKey =  getPublicKey( getExternalSSOUrl() );
                lastPublicKeyFetch = System.currentTimeMillis();
                logger.info("New public key is {}", publicKey);
            }
        }

        if ( claims == null ) {
            logger.error("Error getting Apigee JWT claims", lastException);
            throw new BadTokenException( "Error getting Apigee JWT claims", lastException );
        } else {
            logger.debug( "Success! Got claims for token {} key {}", token, publicKey.toString() );
        }

        return claims;
    }

    public Jws<Claims> getClaims(String token) throws Exception{
        return getClaimsForKeyUrl(token);
    }

    private void validateClaims (final Jws<Claims> claims) throws ExpiredTokenException {

        final Claims body = claims.getBody();

        final long expiry = Long.valueOf(body.get("exp").toString());

        if (expiry - (System.currentTimeMillis()/1000) < 0 ){

            final long expirationDelta = ((System.currentTimeMillis()/1000) - expiry)*1000;

            throw new ExpiredTokenException(String.format("Token expired %d milliseconds ago.", expirationDelta ));
        }

    }


    public void setPublicKey( PublicKey publicKeyArg){
        this.publicKey = publicKeyArg;
    }

    @Autowired
    public void setManagement(ManagementService management) {
        this.management = management;
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
        this.publicKey =  getPublicKey(getExternalSSOUrl());

        lastPublicKeyFetch = System.currentTimeMillis();

        String freshnessString = (String)properties.get( USERGRID_EXTERMAL_PUBLICKEY_FRESHNESS );
        try {
            freshnessTime = Long.parseLong( freshnessString );
        } catch ( Exception e ) {
            logger.error("Ignoring invalid setting for " + USERGRID_EXTERMAL_PUBLICKEY_FRESHNESS );
        }
    }
}
