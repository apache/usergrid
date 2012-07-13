package org.usergrid.rest.management.organizations;

import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.AbstractRestTest;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.usergrid.management.cassandra.ManagementServiceImpl.*;
import static org.usergrid.utils.MapUtils.entry;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * @author zznate
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/testApplicationContext.xml")
public class OrganizationsResourceTest extends AbstractRestTest {

  @Resource
  private EntityManagerFactory emf;

  @Test
  public void createOrgAndOwner() throws Exception {
    properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS,
            "false");
    properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
            "false");
    properties.setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
            "false");
    properties.setProperty(PROPERTIES_SYSADMIN_EMAIL,
            "sysadmin-1@mockserver.com");

    Map<String, String> payload = hashMap("email", "test-user-1@mockserver.com")
            .map("username", "test-user-1").map("name", "Test User")
            .map("password", "password")
            .map("organization", "test-org-1");

    JsonNode node = resource().path("/management/organizations")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(JsonNode.class, payload);

    assertNotNull(node);

    ApplicationInfo applicationInfo = managementService.getApplicationInfo("test-org-1/sandbox");

    assertNotNull(applicationInfo);

    Set<String> rolePerms = emf.getEntityManager(applicationInfo.getId()).getRolePermissions("guest");
    assertNotNull(rolePerms);
    assertTrue(rolePerms.contains("get,post,put,delete:/**"));
    logNode(node);

    UserInfo ui = managementService.getAdminUserByEmail("test-user-1@mockserver.com");
    EntityManager em = emf.getEntityManager(CassandraService.MANAGEMENT_APPLICATION_ID);
    User user = em.get(ui.getUuid(), User.class);
    //assertTrue(user.activated());
    //assertFalse(user.disabled());
    //assertTrue(user.confirmed());
  }

}
