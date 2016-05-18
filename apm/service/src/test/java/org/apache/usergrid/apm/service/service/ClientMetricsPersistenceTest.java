package org.apache.usergrid.apm.service.service;

import junit.framework.TestCase;
import org.apache.usergrid.apm.service.NetworkMetricsDBServiceImpl;
import org.apache.usergrid.apm.service.NetworkTestData;
import org.apache.usergrid.apm.service.ServiceFactory;

public class ClientMetricsPersistenceTest extends TestCase {
	
	protected void setUp() throws Exception {
		super.setUp();
		//TestUtil.deleteLocalDB();
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		//TestUtil.deleteLocalDB();
	}
	
	public void testMetricsRecordPersistence() {
		NetworkMetricsDBServiceImpl ms =  (NetworkMetricsDBServiceImpl) ServiceFactory.getMetricsDBServiceInstance();
		NetworkTestData.populateTestDataforLast1day(999l);
		assertTrue("Verifying records got inserted ", 0 < ms.getRowCount());
		
		
	}

}
