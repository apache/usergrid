package org.usergrid.security.providers;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.entities.User;
import org.usergrid.test.ShiroHelperRunner;
import org.usergrid.utils.MapUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author zznate
 */
@RunWith(ShiroHelperRunner.class)
public class FacebookProviderTest {

  private static FacebookProvider facebookProvider;
  private static ManagementService managementService;
  private static UserInfo adminUser;
 	private static OrganizationInfo organization;
 	private static UUID applicationId;

  private String fb_access_token = "XXXYYYFBTOKENZZZ";



  @BeforeClass
  public static void setup() throws Exception {
    FacebookProvider fp = CassandraRunner.getBean(FacebookProvider.class);
    facebookProvider = Mockito.spy(fp);
    managementService = CassandraRunner.getBean(ManagementService.class);
    adminUser = managementService.createAdminUser("fbuser",
  				"Facebook User", "user@facebook.com", "test", false, false);
  		organization = managementService.createOrganization("fb-organization",
  				adminUser, true);
  		applicationId = managementService.createApplication(
  				organization.getUuid(), "fb-application").getId();
  }

  @Test
  public void verifyGetOrCreateOk() throws Exception {


    Map fb_user = MapUtils
    				.hashMap("id", "12345678").map("name", "Facebook User")
    				.map("username", "fb.user");
    Map<String,String> paramMap = new HashMap<String, String>();
    paramMap.put("access_token", fb_access_token);
    Mockito.when(facebookProvider.userFromResource("https://graph.facebook.com/me",paramMap)).thenReturn(fb_user);


    User user1 = facebookProvider.createOrAuthenticate(applicationId, fb_access_token);
    assertNotNull(user1);

  }

}
