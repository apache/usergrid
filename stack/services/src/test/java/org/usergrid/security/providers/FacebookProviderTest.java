package org.usergrid.security.providers;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.entities.User;
import org.usergrid.test.ShiroHelperRunner;
import org.usergrid.utils.MapUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author zznate
 */
@RunWith(ShiroHelperRunner.class)
public class FacebookProviderTest {

  private static ManagementService managementService;
  private static EntityManagerFactory emf;
  private static SignInProviderFactory providerFactory;
  private static UserInfo adminUser;
 	private static OrganizationInfo organization;
 	private static UUID applicationId;

  private String fb_access_token = "CAAE...NJIZD";



  @BeforeClass
  public static void setup() throws Exception {
    managementService = CassandraRunner.getBean(ManagementService.class);
    emf = CassandraRunner.getBean(EntityManagerFactory.class);
    providerFactory = CassandraRunner.getBean(SignInProviderFactory.class);
    adminUser = managementService.createAdminUser("fbuser",
  				"Facebook User", "user@facebook.com", "test", false, false);
  		organization = managementService.createOrganization("fb-organization",
  				adminUser, true);
  		applicationId = managementService.createApplication(
  				organization.getUuid(), "fb-application").getId();



    //facebookProvider = Mockito.spy(fp);

  }

  @Test
  @Ignore
  public void verifyGetOrCreateOk() throws Exception {
    Application application = emf.getEntityManager(applicationId).getApplication();
    Map fb_user = MapUtils
    				.hashMap("id", "12345678").map("name", "Facebook User")
    				.map("username", "fb.user");

    FacebookProvider facebookProvider = (FacebookProvider)providerFactory.facebook(application);

    User user1 = facebookProvider.createOrAuthenticate(fb_access_token);

    assertNotNull(user1);

  }

  @Test
  public void verifyConfigureOk() throws Exception {
    Application application = emf.getEntityManager(applicationId).getApplication();
    Map fbProps = MapUtils
       				.hashMap("api_url", "localhost");
    //FacebookProvider fp = Mockito.spy((FacebookProvider)providerFactory.facebook(application));
    //Mockito.when(fp.loadConfigurationFor("facebookProvider")).thenReturn(fb_user);
    FacebookProvider fp = (FacebookProvider)providerFactory.facebook(application);
    assertNotNull(fp);

    fp.saveToConfiguration("facebookProvider",fbProps);

    fp.configure();

    Map map = fp.loadConfigurationFor("facebookProvider");
    assertEquals("localhost", map.get("api_url"));

  }

}
