package org.usergrid.rest.management.organizations;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_EMAIL;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import junit.framework.Assert;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.AbstractRestTest;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;

/**
 * @author zznate
 */
public class OrganizationsResourceTest extends AbstractRestTest {


    private EntityManagerFactory emf = CassandraRunner.getBean(EntityManagerFactory.class);

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

        Map<String, Object> organizationProperties = new HashMap<String,Object>();
        organizationProperties.put("securityLevel", 5);

        Map payload = hashMap("email",
                "test-user-1@mockserver.com").map("username", "test-user-1")
                .map("name", "Test User").map("password", "password")
                .map("organization", "test-org-1")
                .map("company","Apigee");
        payload.put(OrganizationsResource.ORGANIZATION_PROPERTIES, organizationProperties);

        JsonNode node = resource().path("/management/organizations")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);

        assertNotNull(node);

        ApplicationInfo applicationInfo = managementService
                .getApplicationInfo("test-org-1/sandbox");

        assertNotNull(applicationInfo);

        Set<String> rolePerms = emf.getEntityManager(applicationInfo.getId())
                .getRolePermissions("guest");
        assertNotNull(rolePerms);
        assertTrue(rolePerms.contains("get,post,put,delete:/**"));
        logNode(node);

        UserInfo ui = managementService
                .getAdminUserByEmail("test-user-1@mockserver.com");
        EntityManager em = emf
                .getEntityManager(CassandraService.MANAGEMENT_APPLICATION_ID);
        User user = em.get(ui.getUuid(), User.class);
        assertEquals("Test User", user.getName());
        assertEquals("Apigee",(String)user.getProperty("company"));

        OrganizationInfo orgInfo = managementService.getOrganizationByName("test-org-1");
        assertEquals(5L, orgInfo.getProperties().get("securityLevel"));

        node = resource().path("/management/organizations/test-org-1")
          .queryParam("access_token", superAdminToken())
          .accept(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .get(JsonNode.class);
        logNode(node);
        Assert.assertEquals(5, node.get("organization").get(OrganizationsResource.ORGANIZATION_PROPERTIES).get("securityLevel").asInt());

        node = resource().path("/management/organizations/test-org-1")
            .queryParam("access_token", superAdminToken())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonNode.class);
        Assert.assertEquals(5, node.get("organization").get(OrganizationsResource.ORGANIZATION_PROPERTIES).get("securityLevel").asInt());
    }

    @Test
    public void testCreateDuplicateOrgName() throws Exception {
        Map<String, String> payload = hashMap("email",
                "create-duplicate-org@mockserver.com").map("password", "password")
                .map("organization", "create-duplicate-orgname-org");

        JsonNode node = resource().path("/management/organizations")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);

        logNode(node);
        assertNotNull(node);

        payload = hashMap("email",
                "create-duplicate-org2@mockserver.com").map("username", "create-dupe-orgname2")
                .map("password", "password")
                .map("organization", "create-duplicate-orgname-org");

        try {
            node = resource().path("/management/organizations")
                        .accept(MediaType.APPLICATION_JSON)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .post(JsonNode.class, payload);

        } catch (Exception ex){ }
        payload = hashMap("grant_type","password")
                .map("username", "create-dupe-orgname2")
                .map("password", "password");
        try {
            node = resource().path("/management/token")
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(JsonNode.class, payload);
            fail("Should not have created user");
        } catch (Exception ex) {}
        logNode(node);

        payload = hashMap("username",
                        "create-duplicate-org@mockserver.com").map("grant_type", "password")
                        .map("password", "password");
        node = resource().path("/management/token")
                            .accept(MediaType.APPLICATION_JSON)
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .post(JsonNode.class, payload);
        logNode(node);
    }

    @Test
    public void testOrgPOSTParams() {
        JsonNode node = resource().path("/management/organizations")
                .queryParam("organization", "testOrgPOSTParams")
                .queryParam("username", "testOrgPOSTParams")
                .queryParam("grant_type", "password")
                .queryParam("email", "testOrgPOSTParams@apigee.com")
                .queryParam("name", "testOrgPOSTParams")
                .queryParam("password", "password")

                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .post(JsonNode.class);

        assertEquals("ok", node.get("status").asText());

    }

    @Test
    public void testOrgPOSTForm() {

        Form form = new Form();
        form.add("organization", "testOrgPOSTForm");
        form.add("username", "testOrgPOSTForm");
        form.add("grant_type", "password");
        form.add("email", "testOrgPOSTForm@apigee.com");
        form.add("name", "testOrgPOSTForm");
        form.add("password", "password");

        JsonNode node = resource().path("/management/organizations")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .post(JsonNode.class, form);

        assertEquals("ok", node.get("status").asText());

    }
    
    @Test
    public void noOrgDelete() {
        
        
        String mgmtToken = adminToken();

        Status status = null;
        JsonNode node = null;
        
        try {
             node = resource().path("/test-organization")
                    .queryParam("access_token", mgmtToken).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.BAD_REQUEST, status);
//        assertEquals("Application delete is not allowed yet", node.get("error_description").asText());
    }
}
