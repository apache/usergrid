package org.usergrid.security.providers;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.tokens.exceptions.BadTokenException;
import org.usergrid.utils.JsonUtils;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.usergrid.utils.ListUtils.anyNull;

/**
 * Foursquare sign-in-as implementation
 *
 * @author zznate
 */
public class FoursquareProvider extends AbstractProvider {

  private Logger logger  = LoggerFactory.getLogger(FoursquareProvider.class);


  FoursquareProvider(EntityManager entityManager,
                     ManagementService managementService) {
    super(entityManager, managementService);
  }

  @Override
  void configure() {
  // config params: url, version

  }

  @Override
  public Map<Object, Object> loadConfigurationFor() {
    return loadConfigurationFor("foursquareProvider");
  }

  @Override
  public void saveToConfiguration(Map<String, Object> config) {
    saveToConfiguration("foursquareProvider", config);
  }

  @Override
  public User createOrAuthenticate(String externalToken) throws BadTokenException {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    Client client = Client.create(clientConfig);
    WebResource web_resource = client.resource("https://api.foursquare.com/v2/users/self");
    Map<String, Object> body = web_resource.queryParam("oauth_token", externalToken).queryParam("v", "20120623")
            .accept(MediaType.APPLICATION_JSON).get(Map.class);

    Map<String, Object> fq_user = (Map<String, Object>) ((Map<?, ?>) body.get("response")).get("user");

    String fq_user_id = (String) fq_user.get("id");
    String fq_user_username = (String) fq_user.get("id");
    String fq_user_email = (String) ((Map<?, ?>) fq_user.get("contact")).get("email");
    String fq_user_picture = (String) ((Map<?, ?>) fq_user.get("photo")).get("suffix");
    String fq_user_name = new String("");

    // Grab the last check-in so we can store that as the user location
    Map<String, Object> fq_location = (Map<String, Object>) ((Map<?, ?>) ((Map<?, ?>) ((ArrayList<?>) ((Map<?, ?>) fq_user
            .get("checkins")).get("items")).get(0)).get("venue")).get("location");

    Map<String, Double> location = new LinkedHashMap<String, Double>();
    location.put("latitude", (Double) fq_location.get("lat"));
    location.put("longitude", (Double) fq_location.get("lng"));

    if ( logger.isDebugEnabled()) {
      logger.debug(JsonUtils.mapToFormattedJsonString(location));
    }

    // Only the first name is guaranteed to be here
    try {
      fq_user_name = (String) fq_user.get("firstName") + " " + (String) fq_user.get("lastName");
    } catch (NullPointerException e) {
      fq_user_name = (String) fq_user.get("firstName");
    }

    User user = null;
    try {
      if ((fq_user != null) && !anyNull(fq_user_id, fq_user_name)) {

        Results r = entityManager.searchCollection(entityManager.getApplicationRef(), "users",
                Query.findForProperty("foursquare.id", fq_user_id));

        if (r.size() > 1) {
          logger.error("Multiple users for FQ ID: " + fq_user_id);
          throw new BadTokenException("multiple users with same Foursquare ID");
        }

        if (r.size() < 1) {
          Map<String, Object> properties = new LinkedHashMap<String, Object>();

          properties.put("foursquare", fq_user);
          properties.put("username", fq_user_username != null ? fq_user_username : "fq_" + fq_user_id);
          properties.put("name", fq_user_name);
          if (fq_user_email != null) {
            properties.put("email", fq_user_email);
          }
          properties.put("picture", "https://is0.4sqi.net/userpix_thumbs" + fq_user_picture);
          properties.put("activated", true);
          properties.put("location", location);

          user = entityManager.create("user", User.class, properties);
        } else {
          user = (User) r.getEntity().toTypedEntity();
          Map<String, Object> properties = new LinkedHashMap<String, Object>();

          properties.put("foursquare", fq_user);
          properties.put("picture", "https://is0.4sqi.net/userpix_thumbs" + fq_user_picture);
          properties.put("location", location);
          entityManager.updateProperties(user, properties);

          user.setProperty("foursquare", fq_user);
          user.setProperty("picture", "https://is0.4sqi.net/userpix_thumbs" + fq_user_picture);
          user.setProperty("location", location);
        }
      } else {
        throw new BadTokenException("Unable to confirm Foursquare access token");
      }
    } catch (Exception ex) {
      throw new BadTokenException("Could not create or update Foursquare user", ex);
    }

    return user;
  }
}
