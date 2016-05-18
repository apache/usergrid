package org.apache.usergrid.apm.service.service;

import java.util.Calendar;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.usergrid.apm.service.NetworkTestData;
import org.apache.usergrid.apm.service.ServiceFactory;
import org.apache.usergrid.apm.service.StatelessComplexEventProcessingService;
import org.drools.runtime.conf.ClockTypeOption;

import org.apache.usergrid.apm.service.charts.service.SessionChartCriteriaService;

public class StatelessComplexEventProcessingServiceTest extends TestCase {

	private static final Log log = LogFactory.getLog(StatelessComplexEventProcessingServiceTest.class);
	
	protected void setUp() throws Exception {
		super.setUp();
		
		NetworkTestData.setupBaselineApplication();
		
		
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	public void testSessionTestingEvents()
	{
		StatelessComplexEventProcessingService cepService = new StatelessComplexEventProcessingService(ClockTypeOption.get("pseudo"));
		cepService.setMetricsDBService(ServiceFactory.getMetricsDBServiceInstance());
		
		SessionChartCriteriaService sessionChartCriteriaService = ServiceFactory.getSessionChartCriteriaService();
		//SessionChartCriteria scc = sessionChartCriteriaService.getDefaultChartCriteriaByName( 1L,SessionChartCriteriaService.ACTIVE_USERS_SESSION);
		
		//Long id = scc.getId();
		
		int numEventPeriods = 60;
		
		Calendar start = Calendar.getInstance();
		start.add(Calendar.HOUR_OF_DAY, -1);
		
		NetworkTestData.populateDataForStatelessComplexEventService(numEventPeriods, start, 5, 1L, cepService);
		
		log.info("------------------------------------------------------------------------------------");

	}
	
	public void testSingleSessionTestingEvents()
	{
		StatelessComplexEventProcessingService cepService = new StatelessComplexEventProcessingService(ClockTypeOption.get("pseudo"));
		//cepService.setMetricsDBService(ServiceFactory.getMetricsDBServiceInstance());
		
		SessionChartCriteriaService sessionChartCriteriaService = ServiceFactory.getSessionChartCriteriaService();
		//SessionChartCriteria scc = sessionChartCriteriaService.getDefaultChartCriteriaByName( 1L,SessionChartCriteriaService.ACTIVE_USERS_SESSION);
		
		//Long id = scc.getId();
		
		int numEventPeriods = 1;
		
		Calendar start = Calendar.getInstance();
		start.add(Calendar.HOUR_OF_DAY, -1);
		
		NetworkTestData.populateDataForStatelessComplexEventService(numEventPeriods, start, 1, 1L, cepService);
		
		log.info("------------------------------------------------------------------------------------");

	}
	
	public void testMultiSessionTestingEvents()
	{
		StatelessComplexEventProcessingService cepService = new StatelessComplexEventProcessingService(ClockTypeOption.get("pseudo"));
		cepService.setMetricsDBService(ServiceFactory.getMetricsDBServiceInstance());
		
		SessionChartCriteriaService sessionChartCriteriaService = ServiceFactory.getSessionChartCriteriaService();
		//SessionChartCriteria scc = sessionChartCriteriaService.getDefaultChartCriteriaByName( 1L,SessionChartCriteriaService.ACTIVE_USERS_SESSION);
		
		//Long id = scc.getId();
		
		int numEventPeriods = 20;
		
		Calendar start = Calendar.getInstance();
		start.add(Calendar.HOUR_OF_DAY, -1);
		
		NetworkTestData.populateDataForStatelessComplexEventService(numEventPeriods, start, 1, 1L, cepService);
		
		log.info("------------------------------------------------------------------------------------");

	}
	
	public void testMultiAppSessionTestingEvents()
	{
		StatelessComplexEventProcessingService cepService = new StatelessComplexEventProcessingService(ClockTypeOption.get("pseudo"));
		cepService.setMetricsDBService(ServiceFactory.getMetricsDBServiceInstance());
		
		SessionChartCriteriaService sessionChartCriteriaService = ServiceFactory.getSessionChartCriteriaService();
		//SessionChartCriteria scc = sessionChartCriteriaService.getDefaultChartCriteriaByName( 1L,SessionChartCriteriaService.ACTIVE_USERS_SESSION);
		
		//Long id = scc.getId();
		
		int numEventPeriods = 5;
		
		Calendar start = Calendar.getInstance();
		start.add(Calendar.HOUR_OF_DAY, -1);
		
		NetworkTestData.populateDataForStatelessComplexEventService(numEventPeriods, start, 1, 2L, cepService);
		
		log.info("------------------------------------------------------------------------------------");

	}

		
}
