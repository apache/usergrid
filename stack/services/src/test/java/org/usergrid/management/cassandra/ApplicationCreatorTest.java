package org.usergrid.management.cassandra;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.usergrid.management.ApplicationCreator;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationOwnerInfo;
import org.usergrid.persistence.EntityManagerFactory;

import javax.annotation.Resource;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author zznate
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/testApplicationContext.xml")
public class ApplicationCreatorTest {

  private static ManagementTestHelperImpl helper;

  @Resource
  private ApplicationCreator applicationCreator;
  @Resource
  private EntityManagerFactory emf;

  private static ManagementService managementService;

  @BeforeClass
 	public static void setup() throws Exception {
 		helper = new ManagementTestHelperImpl();
 		helper.setup();
 		managementService = (ManagementServiceImpl) helper
 				.getManagementService();
 	}

  @Test
  public void testCreateSampleApplication() throws Exception {
    OrganizationOwnerInfo orgOwner = managementService.createOwnerAndOrganization("appcreatortest",
            "nate-appcreatortest", "Nate", "nate+appcreatortest@apigee.com", "password", true, false, false);
    ApplicationInfo appInfo = applicationCreator.createSampleFor(orgOwner.getOrganization());
    assertNotNull(appInfo);
    assertEquals("appcreatortest/sandbox",appInfo.getName());

    Set<String> rolePerms = emf.getEntityManager(appInfo.getId()).getRolePermissions("guest");
    assertNotNull(rolePerms);
    assertTrue(rolePerms.contains("get,post,put,delete:/**"));
  }

  @Test
  public void testCreateSampleApplicationAltName() throws Exception {
    OrganizationOwnerInfo orgOwner = managementService.createOwnerAndOrganization("appcreatortestcustom",
            "nate-appcreatortestcustom", "Nate", "nate+appcreatortestcustom@apigee.com", "password", true, false, false);
    ApplicationCreatorImpl customCreator = new ApplicationCreatorImpl(emf, managementService);
    customCreator.setSampleAppName("messagee");
    ApplicationInfo appInfo = customCreator.createSampleFor(orgOwner.getOrganization());
    assertNotNull(appInfo);
    assertEquals("appcreatortestcustom/messagee",appInfo.getName());
  }
}
