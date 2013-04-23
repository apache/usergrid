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

  private Logger logger = LoggerFactory.getLogger(FacebookProvider.class);



  @Override
  public User createOrAuthenticate(UUID applicationId, String externalToken) throws BadTokenException {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    Client client = Client.create(clientConfig);
    WebResource web_resource = client.resource("https://graph.facebook.com/me");
    @SuppressWarnings("unchecked")
    Map<String, Object> fb_user = web_resource.queryParam("access_token", externalToken)
            .accept(MediaType.APPLICATION_JSON).get(Map.class);
    String fb_user_id = (String) fb_user.get("id");
    String fb_user_name = (String) fb_user.get("name");
    String fb_user_username = (String) fb_user.get("username");
    String fb_user_email = (String) fb_user.get("email");
    if (logger.isDebugEnabled()) {
      logger.debug(JsonUtils.mapToFormattedJsonString(fb_user));
    }
    if (applicationId == null) {
      return null;
    }

    User user = null;

    if ((fb_user != null) && !anyNull(fb_user_id, fb_user_name)) {
      EntityManager em = emf.getEntityManager(applicationId);
      Results r = null;
      try {
        r = em .searchCollection(em.getApplicationRef(), "users", Query.findForProperty("facebook.id", fb_user_id));
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
        properties.put("picture", "http://graph.facebook.com/" + fb_user_id + "/picture");

        if (fb_user_email != null) {
          try {
            user = managementService.getAppUserByIdentifier(applicationId, Identifier.fromEmail(fb_user_email));
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
              em.updateProperties(user, properties);
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
            user = em.create("user", User.class, properties);
          } catch (Exception ex) {
            throw new BadTokenException("Could not create user for that token", ex);
          }

        }
      } else {
        user = (User) r.getEntity().toTypedEntity();
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        properties.put("facebook", fb_user);
        properties.put("picture", "http://graph.facebook.com/" + fb_user_id + "/picture");
        try {
          em.updateProperties(user, properties);

          user.setProperty(PROPERTY_MODIFIED, properties.get(PROPERTY_MODIFIED));
          user.setProperty("facebook", fb_user);
          user.setProperty("picture", "http://graph.facebook.com/" + fb_user_id + "/picture");
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
