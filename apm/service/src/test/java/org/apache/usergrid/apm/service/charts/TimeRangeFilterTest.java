package org.apache.usergrid.apm.service.charts;

import java.util.Calendar;

import junit.framework.TestCase;

import org.apache.usergrid.apm.service.charts.filter.TimeRangeFilter;
import org.apache.usergrid.apm.model.ChartCriteria.LastX;
import org.apache.usergrid.apm.model.MetricsChartCriteria;

public class TimeRangeFilterTest extends TestCase {

	public void testLastHour () {
		MetricsChartCriteria cq = new MetricsChartCriteria ();
		cq.setLastX(LastX.LAST_HOUR);
		TimeRangeFilter trFilter = new TimeRangeFilter(cq);
		assertEquals(trFilter.getPropertyValue(), "endMinute");
		System.out.println(trFilter.getCriteria());
		assertEquals(trFilter.getTo() - trFilter.getFrom(), 60);
	}
	
	public void testLastHourPartial () {
		MetricsChartCriteria cq = new MetricsChartCriteria ();
		cq.setLastX(LastX.LAST_HOUR);
		Calendar start = Calendar.getInstance();
		start.add(Calendar.MINUTE, -60); 
		Long from = start.getTimeInMillis()/1000/60 + 10;	
		TimeRangeFilter trFilter = new TimeRangeFilter(cq);
		trFilter.setFrom(from);
		assertEquals(trFilter.getPropertyValue(), "endMinute");
		System.out.println(trFilter.getCriteria());
		assertEquals(trFilter.getTo() - trFilter.getFrom(), 50);
	}
	
	public void testLastDay ()  {
		MetricsChartCriteria cq = new MetricsChartCriteria ();
		cq.setLastX(LastX.LAST_DAY);
		TimeRangeFilter trFilter = new TimeRangeFilter(cq);
		assertEquals(trFilter.getPropertyValue(), "endHour");
		System.out.println(trFilter.getCriteria());
		assertEquals(24, trFilter.getTo() - trFilter.getFrom());		
	}


}