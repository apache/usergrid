/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
