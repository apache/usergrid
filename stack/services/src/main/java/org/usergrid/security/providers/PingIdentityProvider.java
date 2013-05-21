package org.usergrid.security.providers;

import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.tokens.exceptions.BadTokenException;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provider implementation for accessing Ping Identity
 *
 * @author zznate
 */
public class PingIdentityProvider extends AbstractProvider {

  private Logger logger = LoggerFactory.getLogger(PingIdentityProvider.class);

  private String apiUrl;
  private String clientId;
  private String clientSecret;

  PingIdentityProvider(EntityManager entityManager, ManagementService managementService) {
    super(entityManager, managementService);
  }

  @Override
  public User createOrAuthenticate(String externalToken) throws BadTokenException {
    Map<String, Object> pingUser = userFromResource(externalToken);

    User user = null;
    try {
      user = managementService.getAppUserByIdentifier(entityManager.getApplication().getUuid(),
              Identifier.fromEmail(pingUser.get("username").toString()));
    } catch (Exception ex) {
      ex.printStackTrace();
      // TODO what to do here?
    }

    if ( user == null ) {
      Map<String, Object> properties = new LinkedHashMap<String, Object>();
      properties.putAll(pingUser);
      properties.put("activated", true);
      properties.put("confirmed",true);
      try {
        user = entityManager.create("user", User.class, properties);
      } catch (Exception ex) {
        throw new BadTokenException("Could not create user for that token", ex);
      }

    } else {
      user.setProperty("expiration",pingUser.get("expiration"));
      try {
        entityManager.update(user);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    return user;
  }

  @Override
  void configure() {
    try {
      Map config = loadConfigurationFor();
      if ( config != null ) {
        apiUrl = (String)config.get("api_url");
        clientId = (String)config.get("client_id");
        clientSecret = (String)config.get("client_secret");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public Map<Object, Object> loadConfigurationFor() {
    return loadConfigurationFor("pingIdentProvider");
  }

  @Override
  public void saveToConfiguration(Map<String, Object> config) {
    saveToConfiguration("pingIdentProvider", config);
  }

  @Override
  Map<String, Object> userFromResource(String externalToken) {

    JsonNode node = client.resource(apiUrl).queryParam("grant_type", "urn:pingidentity.com:oauth2:grant_type:validate_bearer")
            .queryParam("client_secret",clientSecret)
            .queryParam("client_id",clientId)
            .queryParam("token",externalToken).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .post(JsonNode.class);

    // {"token_type":"urn:pingidentity.com:oauth2:validated_token","expires_in":5383,
    // "client_id":"dev.app.appservices","access_token":{"subject":"svccastiron@burberry.com","client_id":"dev.app.appservices"}}

    String rawEmail = node.get("access_token").get("subject").getTextValue();

    Map<String,Object> userMap = new HashMap<String, Object>();
    userMap.put("expiration", node.get("expires_in").getLongValue());
    userMap.put("username", pingUsernameFrom(rawEmail));
    userMap.put("name","pinguser");
    userMap.put("email", rawEmail);

    return userMap;
  }

  public static String pingUsernameFrom(String rawEmail) {
    return String.format("pinguser_%s",rawEmail);
  }

  public static long extractExpiration(User user) {
    Long expiration = (Long)user.getProperty("expiration");
    if ( expiration == null ) {
      return 7200;
    }
    return expiration.longValue();
  }
}
