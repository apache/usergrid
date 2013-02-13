package org.usergrid.rest.applications.queues;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.rest.test.resource.TestContext;
import org.usergrid.rest.test.security.TestAdminUser;



public class QueueResourceTest extends AbstractRestTest {

	private static Logger log = LoggerFactory
			.getLogger(QueueResourceTest.class);

	@Test
	public void testInorder() {
	  TestAdminUser testAdmin = new TestAdminUser("queueresourcetest-testInOrder", "queueresourcetest@testInOrder.com", "queueresourcetest@testInOrder.com");
	  
	  //create the text context
	  TestContext context = TestContext.create(this).withOrg("qeueresourcetest").withApp("testInOrder").withUser(testAdmin).createNewOrgAndUser().loginUser();
	  
	  //TODO test
	  context.application().queues();
	  
	  
	  
	  
	  
	

	}

	private class Queue {
	  
	}
}
