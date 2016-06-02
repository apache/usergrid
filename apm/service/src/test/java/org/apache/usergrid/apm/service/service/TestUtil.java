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
package org.apache.usergrid.apm.service.service;

import java.io.File;
import java.util.Calendar;

import org.apache.usergrid.apm.model.ChartCriteria.LastX;
import org.apache.usergrid.apm.model.LogChartCriteria;
import org.apache.usergrid.apm.model.MetricsChartCriteria;
import org.apache.usergrid.apm.model.SessionChartCriteria;
import org.apache.usergrid.apm.service.ApplicationService;
import org.apache.usergrid.apm.service.ServiceFactory;

public class TestUtil {

	public static void clearSQSQueue(Long applicationID) throws InterruptedException
	{
		ApplicationService applicationService = ServiceFactory.getApplicationService();
		
		//applicationService.deleteApplication(appConfigId);
		
	}
	
	public static void deleteLocalDB () {
		TestUtil.deleteDir(new File("/tmp/hypersonic"));

	}

	public static boolean deleteDir(File dir) { 
		if (dir.isDirectory()) {
			String[] children = dir.list(); 
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i])); 
				if (!success) { 
					return false; 
				}
			}
		} // The directory is now empty so delete it return dir.delete();  
		return dir.delete();
	}
	
	public static String generateSampleMessage(int numMessage) {
		String headers="APP_ID,DEVICE_ID,START_TIME,END_TIME,REGEX_URL,NUM_SAMPLES,NUM_ERRORS\n";
		if(numMessage == 0)
			return headers;
		StringBuffer  body = new StringBuffer();
		for (int i =0; i < numMessage; i++) {
			body.append("999,1234,2222222,3333333333,www*goog,1,2\n");
		}
		return headers+body;
	}
	
	public static MetricsChartCriteria getChartCriteriaForHourly() {
		MetricsChartCriteria cq = new MetricsChartCriteria ();
		cq.setAppId(1l);
		cq.setChartName("hourly test chart");		
		cq.setLastX(LastX.LAST_HOUR);
		cq.setDefaultChart(true);
		return cq;
		
	}
	
	public static LogChartCriteria getLogChartCriteriaForHourly() {
      LogChartCriteria cq = new LogChartCriteria ();
      cq.setAppId(1l);
      cq.setChartName("active sessions for a time period");     
      cq.setLastX(LastX.LAST_HOUR);
      cq.setDefaultChart(true);
      cq.setErrorAndAboveCount(1l);
      return cq;
      
   }
	
	public static LogChartCriteria getLogChartCriteriaForHourlyForYesterday() {
      LogChartCriteria cq = new LogChartCriteria ();
      cq.setAppId(1l);
      cq.setChartName("active sessions for a time period");     
      Calendar start = Calendar.getInstance();
      start.add(Calendar.DAY_OF_YEAR, -1); //go back 24 hour
      start.add(Calendar.HOUR_OF_DAY, -1); //then go back 1 hour      
      Calendar end = (Calendar) start.clone(); //end time is 1 hour after start
      end.add(Calendar.HOUR_OF_DAY, 1);   
      cq.setStartDate(start.getTime());
      cq.setEndDate(end.getTime());
      cq.setDefaultChart(true);
      cq.setErrorAndAboveCount(1l);
      return cq;
      
   }
   
   public static LogChartCriteria getGroupByNetworkCarrierLogChartCriteriaForHourly() {
      LogChartCriteria cq = new LogChartCriteria ();
      cq.setAppId(1l);
      cq.setGroupedByNetworkCarrier(true);
      cq.setChartName("active sessions by network carrier");     
      cq.setLastX(LastX.LAST_HOUR);
      cq.setDefaultChart(true);
      cq.setErrorAndAboveCount(1L);
      return cq;
      
   }
	
	public static SessionChartCriteria getActiveSessionChartCriteriaForHourly() {
      SessionChartCriteria cq = new SessionChartCriteria ();
      cq.setAppId(1l);
      cq.setChartName("active sessions for a time period");     
      cq.setLastX(LastX.LAST_HOUR);
      cq.setDefaultChart(true);
      return cq;
      
   }
	
	public static SessionChartCriteria getActiveSessionChartCriteriaForHourlyForYesterday() {
      SessionChartCriteria cq = new SessionChartCriteria ();
      cq.setAppId(1l);
      cq.setChartName("active sessions for a time period");
      Calendar start = Calendar.getInstance();
      start.add(Calendar.DAY_OF_YEAR, -1); //go back 24 hour
      start.add(Calendar.HOUR_OF_DAY, -1); //then go back 1 hour
      
      Calendar end = (Calendar) start.clone(); //end time is 1 hour after start
      end.add(Calendar.HOUR_OF_DAY, 1);   
      cq.setStartDate(start.getTime());
      cq.setEndDate(end.getTime());
      cq.setDefaultChart(true);
      return cq;
      
   }
	
	public static SessionChartCriteria getGroupByNetworkCarrierSessionChartCriteriaForHourly() {
      SessionChartCriteria cq = new SessionChartCriteria ();
      cq.setAppId(1l);
      cq.setGroupedByNetworkCarrier(true);
      cq.setChartName("active sessions by network carrier");     
      cq.setLastX(LastX.LAST_HOUR);
      cq.setDefaultChart(true);
      return cq;
      
   }
	public static MetricsChartCriteria getChartCriteriaForHourlyFixedTime() {
		MetricsChartCriteria cq = new MetricsChartCriteria ();
		cq.setAppId(1l);
		cq.setChartName("fixed hourly test chart");		
		Calendar startTime = Calendar.getInstance();
		startTime.add(Calendar.MINUTE, -59);
		
		Calendar endTime = Calendar.getInstance();
		endTime.add(Calendar.MINUTE, -5);
		cq.setStartDate(startTime.getTime());
		cq.setEndDate(endTime.getTime());
		return cq;
		
	}
	
		
	public static MetricsChartCriteria getChartCriteriaForHourlyNetworkProvider()  {
		MetricsChartCriteria cq = new MetricsChartCriteria ();
		cq.setAppId(1l);
		cq.setChartName("hourly for network provider");		
		cq.setLastX(LastX.LAST_HOUR);
		cq.setGroupedByNeworkProvider(true);
		cq.setDefaultChart(true);
		return cq;
	}
	
	public static SessionChartCriteria getActiveSessionChartCriteriaForDaily() {
	      SessionChartCriteria cq = new SessionChartCriteria ();
	      cq.setAppId(1l);
	      cq.setChartName("active sessions for a time period");     
	      cq.setLastX(LastX.LAST_DAY);
	      cq.setDefaultChart(true);
	      return cq;
	      
	   }

}
