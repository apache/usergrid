package org.usergrid.management.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.providers.FacebookProvider;
import org.usergrid.test.ShiroHelperRunner;
import org.usergrid.utils.MapUtils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;

@PrepareForTest(Client.class)
@RunWith(ShiroHelperRunner.class)
public class ManagementServiceFacebookTest {

	@Rule
	public PowerMockRule rule = new PowerMockRule();

	private static UserInfo adminUser;
	private static OrganizationInfo organization;
	private static UUID applicationId;


	private static FacebookProvider facebookProvider;
  private static ManagementService managementService;

	@BeforeClass
	public static void setup() throws Exception {
		facebookProvider = CassandraRunner.getBean(FacebookProvider.class);
    managementService = CassandraRunner.getBean(ManagementService.class);
		setupLocal();
	}

	public static void setupLocal() throws Exception {
		adminUser = managementService.createAdminUser("fbuser",
				"Facebook User", "user@facebook.com", "test", false, false);
		organization = managementService.createOrganization("fb-organization",
				adminUser, true);
		applicationId = managementService.createApplication(
				organization.getUuid(), "fb-application").getId();
	}

	@Test
	public void getOrCreateUserwithFacebookToken() throws Exception {

		// fb_access_token
		String fb_access_token = "XXXYYYFBTOKENZZZ";

		// setup FB api
		Object[] wrObjs = invokeFB(fb_access_token);
		// process fb_access_token 1st time
		User user1 = facebookProvider.createOrAuthenticate(
				applicationId, fb_access_token);
		assertNotNull(user1);
		verify(wrObjs,fb_access_token);

		// setup FB api
		wrObjs = invokeFB(fb_access_token);
		// process fb_access_token 2nd time
		User user2 = facebookProvider.createOrAuthenticate(
				applicationId, fb_access_token);
		assertNotNull(user2);
		verify(wrObjs,fb_access_token);

		// setup FB api
		wrObjs = invokeFB(fb_access_token);
		// process fb_access_token 3rd time
		User user3 = facebookProvider.createOrAuthenticate(
				applicationId, fb_access_token);
		assertNotNull(user3);
		verify(wrObjs,fb_access_token);

		assertEquals(user1.getUsername(),user2.getUsername());
		assertEquals(user2.getUsername(),user3.getUsername());
		assertEquals(user1.getCreated(),user2.getCreated());
		assertEquals(user2.getCreated(),user3.getCreated());
		// modified values are different
		assertTrue(user2.getModified() > user1.getModified());
		assertTrue(user3.getModified() > user2.getModified());

	}

	private Object[] invokeFB(String fb_access_token) {
		Object[] wrObjs = new Object[2];
		// setup fb_user response
		Map<String, ? extends Object> fb_user = MapUtils
				.hashMap("id", "12345678").map("name", "Facebook User")
				.map("username", "fb.user");

		// mock static client
		PowerMockito.mockStatic(Client.class);
		Client client = mock(Client.class);
		when(Client.create(any(ClientConfig.class))).thenReturn(client);
		when(Client.create(any(ClientConfig.class))).thenReturn(client);

		WebResource wr = mock(WebResource.class);
		doReturn(wr).when(client).resource("https://graph.facebook.com/me");

		doReturn(wr).when(wr).queryParam("access_token", fb_access_token);
		Builder builder = mock(WebResource.Builder.class);
		doReturn(builder).when(wr).accept(MediaType.APPLICATION_JSON);
		doReturn(fb_user).when(builder).get(Map.class);

		wrObjs[0] = wr;
		wrObjs[1] = builder;
		return wrObjs;
	}

	 private void verify(Object[] wrObjs, String fb_access_token) {
		 PowerMockito.verifyStatic();
		 Client.create(any(ClientConfig.class));
		 Mockito.verify((WebResource)wrObjs[0], times(1)).queryParam("access_token", fb_access_token);
		 Mockito.verify((WebResource)wrObjs[0],times(1)).accept(MediaType.APPLICATION_JSON);
		 Mockito.verify((Builder)wrObjs[1], times(1)).get(Map.class);

	 }
}
