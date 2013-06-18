package org.usergrid.security.providers;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.entities.User;
import org.usergrid.test.ShiroHelperRunner;
import org.usergrid.utils.MapUtils;

import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertNotNull;

/**
 * @author zznate
 */
@RunWith(ShiroHelperRunner.class)
public class PingIdentityProviderTest {


  private static ManagementService managementService;
  private static EntityManagerFactory emf;
  private static SignInProviderFactory providerFactory;
  private static UserInfo adminUser;
 	private static OrganizationInfo organization;
 	private static UUID applicationId;



  @BeforeClass
  public static void setup() throws Exception {
    managementService = CassandraRunner.getBean(ManagementService.class);
    emf = CassandraRunner.getBean(EntityManagerFactory.class);
    providerFactory = CassandraRunner.getBean(SignInProviderFactory.class);
    adminUser = managementService.createAdminUser("pinguser",
  				"Ping User", "ping-user@usergrid.com", "test", false, false);
  		organization = managementService.createOrganization("ping-organization",
  				adminUser, true);
  		applicationId = managementService.createApplication(
  				organization.getUuid(), "ping-application").getId();
  }

  @Test
  @Ignore
  public void verifyLiveConnect() throws Exception {
    Application application = emf.getEntityManager(applicationId).getApplication();
    Map pingProps = MapUtils.hashMap("api_url", "")
            .map("client_secret", "")
            .map("client_id", "dev.app.appservicesvalidation");

    PingIdentityProvider pingProvider = (PingIdentityProvider)providerFactory.pingident(application);

    pingProvider.saveToConfiguration(pingProps);
    pingProvider.configure();


    User user = pingProvider.createOrAuthenticate("u0qoW7TS9eT8Vmt7UzrEWrhHbhDK");


    assertNotNull(user);
  }

}
