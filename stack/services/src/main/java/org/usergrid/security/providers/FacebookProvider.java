package org.usergrid.security.providers;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.*;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.tokens.exceptions.BadTokenException;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.MapUtils;

import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.usergrid.persistence.Schema.PROPERTY_MODIFIED;
import static org.usergrid.utils.ListUtils.anyNull;

/**
 * Provider implementation for sign-in-as with facebook
 * @author zznate
 */
public class FacebookProvider extends AbstractProvider {
  private static final String DEF_API_URL = "https://graph.facebook.com/me";
  private static final String DEF_PICTURE_URL  = "http://graph.facebook.com/%s/picture";

  private Logger logger = LoggerFactory.getLogger(FacebookProvider.class);

  private String apiUrl = DEF_API_URL;
  private String pictureUrl   = DEF_PICTURE_URL;

  FacebookProvider(EntityManager entityManager,
                          ManagementService managementService) {
    super(entityManager, managementService);
  }

  @Override
  void configure() {
    try {
      Map config = loadConfigurationFor("facebookProvider");
      if ( config != null ) {
        String foundApiUrl = (String)config.get("api_url");
        if ( foundApiUrl != null ) {
          apiUrl = foundApiUrl;
        }
        String foundPicUrl = (String)config.get("pic_url");
        if ( foundPicUrl != null) {
          pictureUrl = foundApiUrl;
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public Map<Object, Object> loadConfigurationFor() {
    return loadConfigurationFor("facebookProvider");
  }

  /**
   * Configuration parameters we look for:
   * <ul>
   *   <li>api_url</li>
   *   <li>pic_url</li>
   * </ul>
   * @param config
   */
  @Override
  public void saveToConfiguration(Map<String, Object> config) {
    saveToConfiguration("facebookProvider", config);
  }

  @Override
  Map<String, Object> userFromResource(String externalToken) {
    WebResource wr = client.resource(apiUrl);
    wr.queryParam("access_token", externalToken);
    return wr.accept(MediaType.APPLICATION_JSON).post(Map.class);
  }

  @Override
  public User createOrAuthenticate(String externalToken) throws BadTokenException {

    Map<String, Object> fb_user = userFromResource(externalToken);

    String fb_user_id = (String) fb_user.get("id");
    String fb_user_name = (String) fb_user.get("name");
    String fb_user_username = (String) fb_user.get("username");
    String fb_user_email = (String) fb_user.get("email");
    if (logger.isDebugEnabled()) {
      logger.debug(JsonUtils.mapToFormattedJsonString(fb_user));
    }

    User user = null;

    if ((fb_user != null) && !anyNull(fb_user_id, fb_user_name)) {

      Results r = null;
      try {
        r = entityManager .searchCollection(entityManager.getApplicationRef(), "users", Query.findForProperty("facebook.id", fb_user_id));
      } catch (Exception ex) {
        throw new BadTokenException("Could not lookup user for that Facebook ID", ex);
      }
      if (r.size() > 1) {
        logger.error("Multiple users for FB ID: " + fb_user_id);
        throw new BadTokenException("multiple users with same Facebook ID");
      }

      if (r.size() < 1) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        properties.put("facebook", fb_user);
        properties.put("username", "fb_" + fb_user_id);
        properties.put("name", fb_user_name);
        properties.put("picture", String.format(pictureUrl ,fb_user_id ));

        if (fb_user_email != null) {
          try {
            user = managementService.getAppUserByIdentifier(entityManager.getApplication().getUuid(),
                    Identifier.fromEmail(fb_user_email));
          } catch (Exception ex) {
            throw new BadTokenException("Could not find existing user for this applicaiton for email: " + fb_user_email, ex);
          }
          // if we found the user by email, unbind the properties from above
          // that will conflict
          // then update the user
          if (user != null) {
            properties.remove("username");
            properties.remove("name");
            try {
              entityManager.updateProperties(user, properties);
            } catch (Exception ex) {
              throw new BadTokenException("Could not update user with new credentials",ex);
            }
            user.setProperty(PROPERTY_MODIFIED, properties.get(PROPERTY_MODIFIED));
          } else {
            properties.put("email", fb_user_email);
          }
        }
        if (user == null) {
          properties.put("activated", true);
          try {
            user = entityManager.create("user", User.class, properties);
          } catch (Exception ex) {
            throw new BadTokenException("Could not create user for that token", ex);
          }

        }
      } else {
        user = (User) r.getEntity().toTypedEntity();
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        properties.put("facebook", fb_user);
        properties.put("picture", String.format(pictureUrl ,fb_user_id ));
        try {
          entityManager.updateProperties(user, properties);
          user.setProperty(PROPERTY_MODIFIED, properties.get(PROPERTY_MODIFIED));
          user.setProperty("facebook", fb_user);
          user.setProperty("picture", String.format(pictureUrl ,fb_user_id ));
        } catch (Exception ex) {
          throw new BadTokenException("Could not update user properties", ex);
        }
      }
    } else {
      throw new BadTokenException("Unable to confirm Facebook access token");
    }

    return user;
  }
}
