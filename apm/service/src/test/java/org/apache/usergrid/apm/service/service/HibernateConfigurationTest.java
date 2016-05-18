package org.apache.usergrid.apm.service.service;

import junit.framework.TestCase;

import org.apache.usergrid.apm.service.ServiceFactory;
import org.hibernate.Session;

public class HibernateConfigurationTest extends TestCase {
	
	public void setUp() throws Exception {
		super.setUp();
		
	}

	public void tearDown() throws Exception {
		super.tearDown();
		ServiceFactory.getHibernateSession().close();
	}
	
	public void testHibernateConfiguration() {
		Session s = ServiceFactory.getHibernateSession();
		assertTrue(s != null);
		
	}	
		
	


}
