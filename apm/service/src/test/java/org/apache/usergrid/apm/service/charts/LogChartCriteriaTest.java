package org.apache.usergrid.apm.service.charts;

import java.util.List;

import junit.framework.TestCase;

import org.apache.usergrid.apm.model.ChartCriteria.LastX;
import org.apache.usergrid.apm.model.LogChartCriteria;
import org.apache.usergrid.apm.service.ServiceFactory;


public class LogChartCriteriaTest extends TestCase
{
   public void testSaveChartCriteria() {
      LogChartCriteria cq = new LogChartCriteria();
      cq.setAppId(1l);
      cq.setChartName("my super app diagnostic chart crit");      
      cq.setLastX(LastX.LAST_HOUR);
      cq.setGroupedByAppConfigType(true);
      cq.setErrorAndAboveCount(1l);
      Long id = ServiceFactory.getLogChartCriteriaService().saveChartCriteria(cq);
      assertTrue("Chart Criteria was saved with id " + id, id != null);
      assertTrue ("Has grouped by app", ServiceFactory.getLogChartCriteriaService().getChartCriteria(id).isGroupedByAppConfigType());
      
      
   }
   
   public void testGetDefaultChartsForAPP() {
      ServiceFactory.getLogChartCriteriaService().saveDefaultChartCriteriaForApp(20L);
      List <LogChartCriteria> list = ServiceFactory.getLogChartCriteriaService().getDefaultChartCriteriaForApp(20L);
      assertEquals("no of default chart criteria for app", 5, list.size());
      
   }  
   
}

