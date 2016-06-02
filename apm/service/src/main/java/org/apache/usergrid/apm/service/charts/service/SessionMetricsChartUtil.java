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
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;

import org.apache.usergrid.apm.service.charts.filter.AppsFilter;
import org.apache.usergrid.apm.service.charts.filter.EndPeriodFilter;
import org.apache.usergrid.apm.service.charts.filter.NetworkCarrierFilter;
import org.apache.usergrid.apm.service.charts.filter.NetworkTypeFilter;
import org.apache.usergrid.apm.service.charts.filter.SavedChartsFilter;
import org.apache.usergrid.apm.service.charts.filter.SpecialTimeFilter;
import org.apache.usergrid.apm.service.charts.filter.StartPeriodFilter;
import org.apache.usergrid.apm.model.SessionChartCriteria;

public class SessionMetricsChartUtil {

	private static final Log log = LogFactory.getLog(SessionMetricsChartUtil.class);

	public static SqlOrderGroupWhere getOrdersAndGroupings (SessionChartCriteria cq)  {

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
				orders.append("CONFIG_TYPE");
				groupings.append("CONFIG_TYPE");
			}  

			else if (cq.isGroupedByDeviceModel())  {
				orders.append("deviceModel");
				groupings.append("deviceModel");
			}
			else if (cq.isGroupedbyDevicePlatform())  {
				orders.append("devicePlatform");
				groupings.append("devicePlatform");
			} else if (cq.isGroupedByDeviceOS())  {
				orders.append("deviceOperatingSystem");
				groupings.append("deviceOperatingSystem");
			}
			
		} 
		if (!groupings.toString().equals(""))
			groupings.append(',');
		if (!orders.toString().equals(""))
			orders.append(',');     

		orders.append(timeFilter.getEndPropName());
		groupings.append(timeFilter.getEndPropName());


		whereClause.append( timeFilter.getEndPropName() + " >=" + timeFilter.getFrom() + " AND " +
				timeFilter.getEndPropName() + "<" + timeFilter.getTo());


		log.info ("Where clause is " + whereClause.toString());
		log.info ("GroupBy clause is " + groupings.toString());
		log.info ("OrderBy claluse is " + orders.toString());
		og.groupBy = groupings.toString();
		og.orderBy = orders.toString();
		og.whereClause = whereClause.toString();
		return og;

	}

	public static SqlOrderGroupWhere getOrdersAndGroupingsForSummarySessionMetrics (SessionChartCriteria cq)  {

		StringBuffer groupings = new StringBuffer();
		StringBuffer orders = new StringBuffer();
		StringBuffer whereClause = new StringBuffer();
		SqlOrderGroupWhere og = new SqlOrderGroupWhere();
		SpecialTimeFilter timeFilter = new SpecialTimeFilter(cq);

		whereClause.append("appId = " + cq.getAppId() + " AND ");


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
				orders.append("CONFIG_TYPE");
				groupings.append("CONFIG_TYPE");
			}  

			else if (cq.isGroupedByDeviceModel())  {
				orders.append("deviceModel");
				groupings.append("deviceModel");
			}
			else if (cq.isGroupedbyDevicePlatform())  {
				orders.append("devicePlatform");
				groupings.append("devicePlatform");
			} else if (cq.isGroupedByDeviceOS())  {
				orders.append("deviceOperatingSystem");
				groupings.append("deviceOperatingSystem");
			}
		} 
		if (!groupings.toString().equals(""))
			groupings.append(',');
		if (!orders.toString().equals(""))
			orders.append(',');


		//Since index on SummarySessionMetrics is on appId,endMinute, we use endMinute in where clause instead of endHour.
		//The best way I think is to have a background job that updates a different summary session metrics table for Hourly granularity
		orders.append(timeFilter.getEndPropName());
		groupings.append(timeFilter.getEndPropName());

		whereClause.append( "endMinute >=" + timeFilter.getFrom()*60 + " AND " +
				"endMinute <=" + timeFilter.getTo()*60);


		log.info ("Where clause is " + whereClause.toString());
		log.info ("GroupBy clause is " + groupings.toString());
		log.info ("OrderBy claluse is " + orders.toString());
		og.groupBy = groupings.toString();
		og.orderBy = orders.toString();
		og.whereClause = whereClause.toString();
		return og;

	}



	public static List<Criterion> getChartCriteriaForCacheTable(SessionChartCriteria cq) {
		List<Criterion> filters = new ArrayList<Criterion>();
		//Narrow down by Chart Criteria ID and then by time
		if (cq.getId() != null)
			filters.add(new SavedChartsFilter(cq.getId()).getCriteria());
		filters.add(new SpecialTimeFilter(cq).getCriteria());

		return filters;
	}


	public static ProjectionList getProjectionList(SessionChartCriteria cq)
	{
		ProjectionList projList = Projections.projectionList();
		//Adding GroupBy. We will allow only one groupby so that chart looks cleaner.
		if (cq.isGroupedByApp())
		{
			projList.add(Projections.groupProperty("this.appId"),"appId");
		}
		else if (cq.isGroupedByNetworkType())
		{
			projList.add(Projections.groupProperty("this.networkType"),"networkType");
		}

		else if (cq.isGroupedByNetworkCarrier())
		{
			projList.add(Projections.groupProperty("this.networkCarrier"),"networkCarrier");
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





		//may run into this bug because of alias http://stackoverflow.com/questions/84644/hibernate-query-by-example-and-projections
		//And I did run into it. ouch. Fix was to add this.filedName !!

		return projList;

	}

	public static List<Criterion> getChartCriteriaList(SessionChartCriteria cq)
	{

		List<Criterion> filters = new ArrayList<Criterion>();

		//Narrow down by app id first
		if (cq.getAppId() != null)
			filters.add(new AppsFilter(cq.getAppId()).getCriteria());
		//Then by time range
		filters.add(new StartPeriodFilter(cq).getCriteria());
		filters.add(new EndPeriodFilter(cq).getCriteria());


		if (cq.getNetworkCarrier() != null)
			filters.add(new NetworkCarrierFilter(cq.getNetworkCarrier()).getCriteria());     
		if (cq.getNetworkType() != null)
			filters.add(new NetworkTypeFilter(cq.getNetworkType()).getCriteria());


		return filters;
	}

	public static String getWhereClauseForAggregateSessionData(SessionChartCriteria cq) {
		SpecialTimeFilter timeFilter = new SpecialTimeFilter(cq);
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("where appId = " + cq.getAppId() + " AND ");
		whereClause.append(timeFilter.getEndPropName() + " >=" + timeFilter.getFrom() + " AND " +
				timeFilter.getStartPropName() + "<=" + timeFilter.getTo());

		log.info ("where clause for aggregate session data is " + whereClause.toString());

		return whereClause.toString();
	}

	public static String getColumnName (SessionChartCriteria cq) {
		if (cq.hasGrouping()) {
			if (cq.isGroupedByApp())
				return "appId";
			if (cq.isGroupedByAppVersion()) 
				return "applicationVersion";
			if (cq.isGroupedByNetworkType())
				return "networkType";
			if (cq.isGroupedByNetworkCarrier())
				return "networkCarrier";
			if (cq.isGroupedByAppVersion())
				return "applicationVersion";
			if (cq.isGroupedByAppConfigType())
				return "CONFIG_TYPE";
			if (cq.isGroupedByDeviceModel())  
				return "deviceModel";
			if (cq.isGroupedbyDevicePlatform())  
				return "devicePlatform";
			if(cq.isGroupedByDeviceOS())
				return "deviceOperatingSystem";
		} 
		return null;

	}

}
