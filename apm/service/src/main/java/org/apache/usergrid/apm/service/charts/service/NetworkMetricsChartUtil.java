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
package org.apache.usergrid.apm.service.charts.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import org.apache.usergrid.apm.service.charts.filter.AppsFilter;
import org.apache.usergrid.apm.service.charts.filter.HasErrorFilter;
import org.apache.usergrid.apm.service.charts.filter.MinLatencyFilter;
import org.apache.usergrid.apm.service.charts.filter.NetworkCarrierFilter;
import org.apache.usergrid.apm.service.charts.filter.NetworkTypeFilter;
import org.apache.usergrid.apm.service.charts.filter.SavedChartsFilter;
import org.apache.usergrid.apm.service.charts.filter.SpecialTimeFilter;
import org.apache.usergrid.apm.service.charts.filter.TimeRangeFilter;
import org.apache.usergrid.apm.service.charts.filter.UrlFilter;
import org.apache.usergrid.apm.model.MetricsChartCriteria;

public class NetworkMetricsChartUtil {
	private static final Log log = LogFactory.getLog(NetworkMetricsChartUtil.class);


	public static SqlOrderGroupWhere getOrdersAndGroupings (MetricsChartCriteria cq)  {
		StringBuffer groupings = new StringBuffer();
		StringBuffer orders = new StringBuffer();
		StringBuffer whereClause = new StringBuffer();
		SqlOrderGroupWhere og = new SqlOrderGroupWhere();
		SpecialTimeFilter timeFilter = new SpecialTimeFilter(cq);

		whereClause.append("appId = " + cq.getAppId() + " AND chartCriteriaId = " + cq.getId() + " AND ");


		if (cq.hasGrouping()) {

			if (cq.isGroupedByApp())
			{
				orders.append("appId");
				groupings.append("appId");
			}

			else if (cq.isGroupedByNetworkType())
			{
				orders.append("networkType");
				groupings.append("networkType");
			}

			else if (cq.isGroupedByNetworkCarrier())
			{
				orders.append("networkCarrier");
				groupings.append("networkCarrier");

			}

			else if (cq.isGroupedByAppVersion()) {
				orders.append("applicationVersion");
				groupings.append("applicationVersion");
			}
			else if (cq.isGroupedByAppConfigType()) {
				orders.append("appConfigType");
				groupings.append("appConfigType");
			}  

			else if (cq.isGroupedByDeviceModel())  {
				orders.append("deviceModel");
				groupings.append("deviceModel");
			}
			else if (cq.isGroupedbyDevicePlatform())  {
				orders.append("devicePlatform");
				groupings.append("devicePlatform");
			}
			else if (cq.isGroupedByDeviceOS())  {
				orders.append("deviceOperatingSystem");
				groupings.append("deviceOperatingSystem");
			}
			else if (cq.isGroupedByDomain())  {
				orders.append("domain");
				groupings.append("domain");
			}
		} 
		if (!groupings.toString().equals(""))
			groupings.append(',');
		if (!orders.toString().equals(""))
			orders.append(',');

		orders.append(timeFilter.getEndPropName());
		groupings.append(timeFilter.getEndPropName());
		whereClause.append(timeFilter.getEndPropName() + " >=" + timeFilter.getFrom() + " AND " +
				timeFilter.getEndPropName() + "<" + timeFilter.getTo());

		log.info ("Where clause is " + whereClause.toString());
		log.info ("GroupBy clause is " + groupings.toString());
		log.info ("OrderBy claluse is " + orders.toString());
		og.groupBy = groupings.toString();
		og.orderBy = orders.toString();
		og.whereClause = whereClause.toString();
		return og;

	}

	public static List<Order> getOrdersForChartCriteria (MetricsChartCriteria cq) {
		List <Order> orders = new ArrayList <Order> ();

		if (cq.isGroupedByApp())
		{
			orders.add(Order.asc("appId"));
		}

		else if (cq.isGroupedByNetworkType())
		{
			orders.add(Order.asc("networkType"));
		}

		else if (cq.isGroupedByNetworkCarrier())
		{
			orders.add(Order.asc("networkCarrier"));
		}

		else if (cq.isGroupedByAppVersion()) {
			orders.add(Order.asc("applicationVersion"));                
		}
		else if (cq.isGroupedByAppConfigType()) {
			orders.add(Order.asc("appConfigType"));        
		}  
		else if (cq.isGroupedByDeviceModel())  {
			orders.add(Order.asc("deviceModel"));         
		}
		else if (cq.isGroupedbyDevicePlatform())  {
			orders.add(Order.asc("devicePlatform"));
		}

		switch (cq.getSamplePeriod())
		{
		//see http://stackoverflow.com/questions/84644/hibernate-query-by-example-and-projections on why "this." is needed 
		//in following lines
		case MINUTE : orders.add(Order.asc("endMinute")); break;
		case HOUR : orders.add(Order.asc("endHour"));break;
		case DAY_WEEK : orders.add(Order.asc("endDay"));break;
		case DAY_MONTH : orders.add(Order.asc("endDay"));break;
		case MONTH : orders.add(Order.asc("endMonth"));break;
		}

		return orders;

	}

	public static List<Criterion> getChartCriteriaList(MetricsChartCriteria cq)
	{

		List<Criterion> filters = new ArrayList<Criterion>();

		//Narrow down by app id first
		if (cq.getAppId() != null)
			filters.add(new AppsFilter(cq.getAppId()).getCriteria());
		//Then by time range
		filters.add(NetworkMetricsChartUtil.getTimeInterval(cq));

		if (cq.isErrored())
			filters.add(new HasErrorFilter().getCriteria());

		if (cq.getUrl() != null)
			filters.add(new UrlFilter(cq.getUrl().trim()).getCriteria());		
		if (cq.getNetworkCarrier() != null)
			filters.add(new NetworkCarrierFilter(cq.getNetworkCarrier().trim()).getCriteria());		
		if (cq.getNetworkType() != null)
			filters.add(new NetworkTypeFilter(cq.getNetworkType().trim()).getCriteria());
		if (cq.getLowerLatency() != null && cq.getLowerLatency() > 0) 
			filters.add(new MinLatencyFilter(cq.getLowerLatency()).getCriteria());

		//filters.add(Restrictions.ge("", value))
		//TODO: Need to add SQL query for ge or le for avg latency
		//TODO: Need to add for Errors
		//TODO: need to add for lattitue and longitude


		return filters;
	}

	public static List <Criterion> getChartCriteriaWithOverRiddenStartTime (MetricsChartCriteria cq, Long startOverwrite) {
		List<Criterion> filters = new ArrayList<Criterion>();

		//Narrow down by app id first
		if (cq.getAppId() != null)
			filters.add(new AppsFilter(cq.getAppId()).getCriteria());
		//Then by time range
		filters.add(NetworkMetricsChartUtil.getTimeInterval(cq, startOverwrite));

		if (cq.isErrored())
			filters.add(new HasErrorFilter().getCriteria());

		if (cq.getUrl() != null)
			filters.add(new UrlFilter(cq.getUrl()).getCriteria());		
		if (cq.getNetworkCarrier() != null)
			filters.add(new NetworkCarrierFilter(cq.getNetworkCarrier()).getCriteria());		
		if (cq.getNetworkType() != null)
			filters.add(new NetworkTypeFilter(cq.getNetworkType()).getCriteria());
		if (cq.getLowerLatency() != null && cq.getLowerLatency() > 0) 
			filters.add(new MinLatencyFilter(cq.getLowerLatency()).getCriteria());


		//filters.add(Restrictions.ge("", value))
		//TODO: Need to add SQL query for ge or le for avg latency
		//TODO: Need to add for Errors
		//TODO: need to add for lattitue and longitude


		return filters;


	}


	public static ProjectionList getProjectionList(MetricsChartCriteria cq)
	{
		ProjectionList projList = Projections.projectionList();
		//Adding GroupBy. We will allow only one groupby so that chart looks cleaner.
		if (cq.isGroupedByApp())
		{
			projList.add(Projections.groupProperty("this.appId"),"appId");
		}
		else 
			projList.add(Projections.property("this.appId"),"appId");

		//projList.add(Projections.groupProperty("appId"),"appId");

		if (cq.isGroupedByNetworkType())
		{
			projList.add(Projections.groupProperty("this.networkType"),"networkType");
		}

		else if (cq.isGroupedByNetworkCarrier())
		{
			projList.add(Projections.groupProperty("this.networkCarrier"),"networkCarrier");
		}

		else if (cq.isGroupedByAppVersion()) {
			projList.add(Projections.groupProperty("this.applicationVersion"),"applicationVersion");        
		}
		else if (cq.isGroupedByAppConfigType()) {
			projList.add(Projections.groupProperty("this.appConfigType"),"appConfigType");        
		}  
		else if (cq.isGroupedByDeviceModel())  {
			projList.add(Projections.groupProperty("this.deviceModel"),"deviceModel");         
		}
		else if (cq.isGroupedbyDevicePlatform())  {
			projList.add(Projections.groupProperty("this.devicePlatform"),"devicePlatform");
		}


		switch (cq.getSamplePeriod())
		{
		//see http://stackoverflow.com/questions/84644/hibernate-query-by-example-and-projections on why "this." is needed 
		//in following lines
		case MINUTE : projList.add(Projections.groupProperty("this.endMinute"),"endMinute"); break;
		case HOUR : projList.add(Projections.groupProperty("this.endHour"),"endHour");break;
		case DAY_WEEK : projList.add(Projections.groupProperty("this.endDay"),"endDay");break;
		case DAY_MONTH : projList.add(Projections.groupProperty("this.endDay"),"endDay");break;
		case MONTH : projList.add(Projections.groupProperty("this.endMonth"),"endMonth");break;
		}

		//Adding Projections


		projList.add(Projections.sum("numSamples"),"numSamples");
		projList.add(Projections.sum("numErrors"),"numErrors");
		projList.add(Projections.sum("sumLatency"),"sumLatency");
		projList.add(Projections.max("maxLatency"),"maxLatency");
		projList.add(Projections.min("minLatency"),"minLatency");

		//may run into this bug because of alias http://stackoverflow.com/questions/84644/hibernate-query-by-example-and-projections
		//And I did run into it. ouch. Fix was to add this.filedName !!

		return projList;

	}


	public static Criterion getTimeInterval (MetricsChartCriteria cq) {
		/*if (cq.getPeriodType().equals(PeriodType.SET_PERIOD)) {
			//TODO:take care of numRowstoGet in this case
			return new DateIntervalFilter(cq.getStartDate(), cq.getEndDate()).getCriteria();			
		}
		else*/
		return new TimeRangeFilter(cq).getCriteria();
	}

	public static Criterion getTimeInterval (MetricsChartCriteria cq, Long startOverwrite)  {
		/*if (cq.getPeriodType().equals(PeriodType.SET_PERIOD)) {
			//TODO:take care of startOverwrite in this case but don't think this is needed
			return new DateIntervalFilter(cq.getStartDate(), cq.getEndDate()).getCriteria();			
		}
		else  {*/
		TimeRangeFilter trf = new TimeRangeFilter(cq);
		if (startOverwrite != 0)
			trf.setFrom(startOverwrite);
		return trf.getCriteria(); 
		//}

	}

	public static List<Criterion> getChartCriteriaForCacheTable(MetricsChartCriteria cq) {
		List<Criterion> filters = new ArrayList<Criterion>();
		//Narrow down by Chart Criteria ID
		if (cq.getId() != null)
			filters.add(new SavedChartsFilter(cq.getId()).getCriteria());
		filters.add(new TimeRangeFilter(cq).getCriteria());
		return filters;
	}

	public static String getQueryForAggregatedData (MetricsChartCriteria cq) {

		// SELECT  sum(sum_latency)/sum(num_samples) as avgLatency, max(max_latency) as maxLatency, 
		//sum(num_samples) as totReq, sum(num_errors) as totErrors FROM `ideawheel`.`HOURLY_COMPACT_NETWORK_METRICS
		//where ...
		return null;
	}


	public static List<Criterion> getRawNetworkMetricsCriteriaList(NetworkMetricsRawCriteria rawCriteria)
	{

		MetricsChartCriteria cq = rawCriteria.getChartCriteria();
		List<Criterion> filters = new ArrayList<Criterion>();

		//Narrow down by app id first
		if (cq.getAppId() != null)
			filters.add(new AppsFilter(cq.getAppId()).getCriteria());
		//Then by time range
		filters.add(NetworkMetricsChartUtil.getTimeInterval(cq));
		
		if (rawCriteria.getHttpStatusCode() != null && rawCriteria.getHttpStatusCode() > 0)
			filters.add(Restrictions.eq("httpStatusCode", rawCriteria.getHttpStatusCode()));
		
		if (cq.getDeviceId() != null) 
			filters.add(Restrictions.like("deviceId", cq.getDeviceId().trim(),MatchMode.START));
		
		if (cq.getNetworkCarrier() != null)
			filters.add(Restrictions.like("networkCarrier", cq.getNetworkCarrier().trim(), MatchMode.START));     
		if (cq.getNetworkType() != null)
			filters.add(Restrictions.like("networkType", cq.getNetworkType().trim(), MatchMode.START));
		
		if (cq.getAppVersion() != null)
			filters.add(Restrictions.like("applicationVersion", cq.getAppVersion().trim(),MatchMode.START));   
		if (cq.getAppConfigType() != null)
			filters.add(Restrictions.like("appConfigType", cq.getAppConfigType(),MatchMode.START));
		if (cq.getDevicePlatform() != null)
			filters.add(Restrictions.like("devicePlatform", cq.getDevicePlatform().trim(), MatchMode.START));    
		if (cq.getDeviceModel() != null)
			filters.add(Restrictions.like("deviceModel", cq.getDeviceModel().trim(),MatchMode.START));
		if (cq.getDeviceOS() != null)
			filters.add(Restrictions.like("deviceOperatingSystem", cq.getDeviceOS().trim(), MatchMode.START)); 

		if (cq.getUpplerLatency() != null && cq.getUpplerLatency() > 0) 
			filters.add(Restrictions.ge("latency", cq.getUpplerLatency()));
		if (cq.getUpperErrorCount() != null && cq.getUpperErrorCount() > 0) 
			filters.add(Restrictions.ge("numErrors", cq.getUpperErrorCount()));
		if (cq.getDevicePlatform() != null)
		
		if (cq.getUrl() != null)
			filters.add(new UrlFilter(cq.getUrl().trim()).getCriteria());
		return filters;
	}

}
