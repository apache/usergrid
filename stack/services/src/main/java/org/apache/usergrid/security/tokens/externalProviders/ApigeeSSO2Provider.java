package org.apache.usergrid.security.tokens.externalProviders;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.tokens.TokenInfo;
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
import java.util.Map;
import java.util.Properties;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

/**
 * Created by ayeshadastagiri on 6/22/16.
 */
public class ApigeeSSO2Provider implements ExternalTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(ApigeeSSO2Provider.class);
    private static final String RESPONSE_PUBLICKEY_VALUE = "value";
    protected Properties properties;
    protected ManagementService management;
    protected Client client;
    protected String publicKey;

    public static final String USERGRID_EXTERNAL_PUBLICKEY_URL = "usergrid.external.sso.publicKeyUrl";

    public ApigeeSSO2Provider() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JacksonFeature());
        client = ClientBuilder.newClient(clientConfig);
    }

    private String getPublicKey() {
        Map<String, Object> publicKey = client.target(properties.getProperty(USERGRID_EXTERNAL_PUBLICKEY_URL)).request().get(Map.class);
        return publicKey.get(RESPONSE_PUBLICKEY_VALUE).toString().split("----\n")[1].split("\n---")[0];
    }

    @Override
    public TokenInfo validateAndReturnTokenInfo(String token, long ttl) throws Exception {

        try {
            UserInfo userInfo = validateAndReturnUserInfo(token, ttl);
            TokenInfo tokeninfo = new TokenInfo(UUIDUtils.newTimeUUID(), "access", 1, 1, 1, ttl,
                new AuthPrincipalInfo(AuthPrincipalType.ADMIN_USER, userInfo.getUuid(),
                    CpNamingUtils.MANAGEMENT_APPLICATION_ID), null);
            return tokeninfo;
        }
        catch(Exception e){
            logger.debug("Error construcing token info from userinfo");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public UserInfo validateAndReturnUserInfo(String token, long ttl) throws Exception {

        byte[] publicBytes = decodeBase64(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        Jws<Claims> payload = null;
        try {
            payload = Jwts.parser().setSigningKey(pubKey).parseClaimsJws(token);
            UserInfo userInfo = management.getAdminUserByEmail(payload.getBody().get("email").toString());
            if (userInfo == null) {
                throw new IllegalArgumentException("user " + payload.getBody().get("email").toString() + " doesnt exist");
            }
            return userInfo;
        } catch (SignatureException se) {
            logger.debug("Signature did not match.");
            throw new IllegalArgumentException("Signature did not match for the token.");
        } catch (Exception e) {
            logger.debug("Error validating Apigee SSO2 token.");
            e.printStackTrace();
        }
        return null;
    }


    @Autowired
    public void setManagement(ManagementService management) {
        this.management = management;
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
        this.publicKey = getPublicKey();
    }
}
