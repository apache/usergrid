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
        return client.target(properties.getProperty(USERGRID_EXTERNAL_PUBLICKEY_URL)).request().get(Map.class).
            get(RESPONSE_PUBLICKEY_VALUE).toString();
    }

    @Override
    public TokenInfo validateAndReturnUserInfo(String token) throws Exception {


        //todo:// check cache , if its invalid retrieve from the USERGRID_EXTERNAL_PUBLICKEY_URL
        String key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0w+/0m5ENuLEAUxPxTMm\nXrJ8bhkyPunC2VCLTaA+2dkYhR0pfPM4RtCO9tqkjc63Raos3OTph9I9gE7RMBiU\n0GVMBDYe74OLZjpGeI7OO8TmQhaOeLzX/ej9QBvq1gbhHt1QGP3m43/g1bN64Ggt\nBq4NCXm1Ie80NvdxWDsKifhYi4fgo+zhcMBaSE2Hhyc3TIg1oEKfh+EmHL/4LhPd\n7CYl4PxqR+DVNbJrdGeGOteWX4p5sW79t/8CsnvJ4St5Yv3sGK5JuBbmGiKW8wWE\nn+9bDg5i3SPlimMBdySH+wMbFULyfVSvJHeMmEAMHKDq+PVGdM+znNkPCCVIkHZG\nRQIDAQAB";
        byte[] publicBytes = decodeBase64(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        Jws<Claims> payload = null;
        try {
            payload = Jwts.parser().setSigningKey(pubKey).parseClaimsJws(token);
            System.out.println(payload.getBody());
            //todo : construct the jsonNode object - or should we construct the Tokeninfo??
            UserInfo userInfo = management.getAdminUserByEmail(payload.getBody().get("email").toString());
            TokenInfo tokeninfo = new TokenInfo(UUIDUtils.newTimeUUID(), "access", 1, 1, 1, 1,
                new AuthPrincipalInfo(AuthPrincipalType.ADMIN_USER, userInfo.getUuid(),
                    CpNamingUtils.MANAGEMENT_APPLICATION_ID), null);
            return tokeninfo;
        } catch (SignatureException se) {
            //if this exception is thrown, first check if the token is expired. Else retirve the public key from the EXTERNAL_URL
            try{
                getPublicKey();
            }
            catch(Exception e){

            }
            logger.debug("signature did not match");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    @Autowired
    public void setManagement(ManagementService management) {
        this.management = management;
    }

    @Autowired
    public void setProperties( Properties properties ) {
        this.properties = properties;
        this.publicKey = getPublicKey();
    }
}
