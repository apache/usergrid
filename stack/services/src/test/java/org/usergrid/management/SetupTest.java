package org.usergrid.management;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import javax.mail.Message;

import me.prettyprint.hector.testutils.EmbeddedServerHelper;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.usergrid.management.cassandra.ManagementServiceImpl;
import org.usergrid.management.cassandra.ManagementTestHelperImpl;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImpl;

public class SetupTest {

  private static final Logger logger = LoggerFactory.getLogger(SetupTest.class);

  @Test
  public void testEmail() throws Exception {
    ManagementTestHelper helper = new MyManagementTestHelperImpl();
    helper.setup();
    ManagementServiceImpl management = (ManagementServiceImpl)helper.getManagementService();
    Properties properties = helper.getProperties();

    properties.setProperty("usergrid.sysadmin.email", "test@email.com");
    properties.setProperty("usergrid.management.admin_users_require_confirmation", "true");
    properties.setProperty("usergrid.management.admin_users_require_activation", "true");
    properties.setProperty("usergrid.management.notify_admin_of_activation", "true");
    properties.setProperty("usergrid.management.organizations_require_confirmation", "false");
    properties.setProperty("usergrid.management.organizations_require_activation", "true");
    properties.setProperty("usergrid.management.notify_sysadmin_of_new_organizations", "false");
    properties.setProperty("usergrid.management.notify_sysadmin_of_new_admin_users", "true");

    management.setProperties(properties);
    management.setup();

    String email = properties.getProperty("usergrid.sysadmin.email");
    List<Message> inbox = org.jvnet.mock_javamail.Mailbox.get(email);
    assertFalse(inbox.isEmpty());

    helper.teardown();
  }

  @Component
  static class MyManagementTestHelperImpl extends ManagementTestHelperImpl {

    @Override
    public void setup() throws Exception {
      // assertNotNull(client);

      String maven_opts = System.getenv("MAVEN_OPTS");
      logger.info("Maven options: " + maven_opts);

      logger.info("Starting Cassandra");
      EmbeddedServerHelper embedded = new EmbeddedServerHelper();
      embedded.setup();

      // copy("/testApplicationContext.xml", TMP);

      String[] locations = { "usergrid-test-context.xml" };
      ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(locations);

      AutowireCapableBeanFactory acbf = ac.getAutowireCapableBeanFactory();
      acbf.autowireBeanProperties(this,
              AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
      acbf.initializeBean(this, "testClient");

      EntityManagerFactory emf = getEntityManagerFactory();
      assertNotNull(emf);
      assertTrue(
              "EntityManagerFactory is instance of EntityManagerFactoryImpl",
              emf instanceof EntityManagerFactoryImpl);

      emf.setup();

      // we're overriding this class just to comment out this line...
//      management.setup();
    }

  }
}
