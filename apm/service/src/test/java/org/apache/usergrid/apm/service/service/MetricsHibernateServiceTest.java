package org.apache.usergrid.apm.service.service;

import junit.framework.TestCase;
import org.apache.usergrid.apm.service.NetworkTestData;


public class MetricsHibernateServiceTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		
		//NetworkTestData.populateTestDB(1L);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		
	}

	public void testTestData()
	{
		NetworkTestData.populateTestDataforLast1day(1L);
	}
}
