package org.usergrid.security.providers;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author zznate
 */
public abstract class AbstractProvider implements SignInAsProvider {

  protected EntityManagerFactory emf;
  protected ManagementService managementService;

  @Autowired
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Autowired
  public void setManagementService(ManagementService managementService) {
    this.managementService = managementService;
  }

  protected Map<String,Object> userFromResource(String url, Map<String,String> queryParams){
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    Client client = Client.create(clientConfig);
    WebResource wr = client.resource(url);

    for ( Map.Entry<String,String> entry : queryParams.entrySet() ) {
      wr.queryParam(entry.getKey(), entry.getValue());
    }

    return wr.accept(MediaType.APPLICATION_JSON).get(Map.class);
  }

}
