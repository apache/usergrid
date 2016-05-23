package org.apache.usergrid.apm.service.charts;

import java.util.List;

import junit.framework.TestCase;

import org.apache.usergrid.apm.service.charts.service.NetworkMetricsChartCriteriaService;
import org.apache.usergrid.apm.model.ChartCriteria.LastX;
import org.apache.usergrid.apm.model.MetricsChartCriteria;
import org.apache.usergrid.apm.service.ServiceFactory;

public class NetworkMetricsChartCriteriaTest extends TestCase {

	
	
	public void testSaveChartCriteria() {
		MetricsChartCriteria cq = new MetricsChartCriteria ();
		cq.setAppId(1l);
		cq.setChartName("my super chart crit");		
		cq.setLastX(LastX.LAST_HOUR);
		cq.setGroupedByApp(true);
		Long id = ServiceFactory.getNetworkMetricsChartCriteriaService().saveChartCriteria(cq);
		assertTrue ("Chart Criteria was saved with id " + id, id != null);
		
		
	}
	
	public void testGetDefaultChartsForAPP() {
		ServiceFactory.getNetworkMetricsChartCriteriaService().saveDefaultChartCriteriaForApp(20L);
		List <MetricsChartCriteria> list = ServiceFactory.getNetworkMetricsChartCriteriaService().getDefaultChartCriteriaForApp(20L);
		assertEquals("no of default chart criteria for app", 16, list.size());
		
	}
	
	public void testSimple() {
		MetricsChartCriteria cq = new MetricsChartCriteria ();
		System.out.println(cq.getSamplePeriod());
	}
	
	public void testSaveGetChartForUser()  {
		MetricsChartCriteria cq1 = new MetricsChartCriteria ();
		cq1.setUserName(("chart-user0"));
		
		MetricsChartCriteria cq2 = new MetricsChartCriteria ();
		cq2.setUserName(("chart-user1"));
		
		MetricsChartCriteria cq3 = new MetricsChartCriteria ();
		cq3.setUserName(("chart-user0"));
		
		NetworkMetricsChartCriteriaService cqs = ServiceFactory.getNetworkMetricsChartCriteriaService();
		cqs.saveChartCriteria(cq1);
		cqs.saveChartCriteria(cq2);
		cqs.saveChartCriteria(cq3);
		
		List<MetricsChartCriteria> cqFromDB = cqs.getChartCriteriaForUser("chart-user0");
		assertEquals("saved 2 and got : " , 2, cqFromDB.size());
	}
	
	

}
