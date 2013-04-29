package org.usergrid.security.providers;

import com.sun.jersey.api.client.Client;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.test.ShiroHelperRunner;

import java.util.UUID;

/**
 * @author zznate
 */
@PrepareForTest(Client.class)
@RunWith(ShiroHelperRunner.class)
public class FacebookProviderTest {

  private FacebookProvider facebookProvider;
  private ManagementService managementService;

  private static UserInfo adminUser;
  private static OrganizationInfo organization;
  private static UUID applicationId;


  @Before
  public void setupLocal() {
    facebookProvider = CassandraRunner.getBean(FacebookProvider.class);
    managementService = CassandraRunner.getBean(ManagementService.class);
  }

}
