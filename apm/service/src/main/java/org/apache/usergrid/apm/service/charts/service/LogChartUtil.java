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
import org.apache.usergrid.apm.service.charts.filter.EndPeriodFilter;
import org.apache.usergrid.apm.service.charts.filter.NetworkCarrierFilter;
import org.apache.usergrid.apm.service.charts.filter.NetworkTypeFilter;
import org.apache.usergrid.apm.service.charts.filter.SavedChartsFilter;
import org.apache.usergrid.apm.service.charts.filter.SpecialTimeFilter;
import org.apache.usergrid.apm.service.charts.filter.StartPeriodFilter;
import org.apache.usergrid.apm.service.charts.filter.TimeRangeFilter;
import org.apache.usergrid.apm.model.LogChartCriteria;


public class LogChartUtil {

	private static final Log log = LogFactory.getLog(LogChartUtil.class);

	public static SqlOrderGroupWhere getOrdersAndGroupings (LogChartCriteria cq)  {
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

	public static String getOrder (LogChartCriteria cq) {
		String order = null;
		switch (cq.getSamplePeriod())
		{
		//see http://stackoverflow.com/questions/84644/hibernate-query-by-example-and-projections on why "this." is needed 
		//in following lines
		case MINUTE : 
			order = "endMinute";
			break;
		case HOUR : 
			order = "endHour";
			break;
		case DAY_WEEK :
			order = "endDay";
			break;
		case DAY_MONTH : 
			order = "endWeek";
			break;
		case MONTH : 
			order = "endMonth";
			break;
		}

		return order;

	}

	public static List<Criterion> getChartCriteriaForCacheTable(LogChartCriteria cq) {
		List<Criterion> filters = new ArrayList<Criterion>();
		//Narrow down by Chart Criteria ID and then by time
		if (cq.getId() != null)
			filters.add(new SavedChartsFilter(cq.getId()).getCriteria());
		filters.add(new SpecialTimeFilter(cq).getCriteria());

		return filters;
	}

	public static List<Order> getOrdersForChartCriteria (LogChartCriteria cq) {
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
		else if (cq.isGroupedByDeviceOS())  {
			orders.add(Order.asc("deviceOperatingSystem"));
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


	public static ProjectionList getProjectionList(LogChartCriteria cq)
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

	public static List<Criterion> getChartCriteriaList(LogChartCriteria cq)
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

	public static SqlOrderGroupWhere getSqlStuff (LogRawCriteria lrc) {
		SqlOrderGroupWhere ogw = new SqlOrderGroupWhere();
		// SELECT count(*), log_level, log_message, max(time_stamp) from  `ideawheel`.`CLIENT_LOG` 
		//where app_Id=1 group by log_message order by log_level desc
		Long app_id = lrc.getChartCriteria().getAppId();
		String applicationVersion = lrc.getChartCriteria().getAppVersion();
		SpecialTimeFilter timeFilter = new SpecialTimeFilter(lrc.getChartCriteria());

		StringBuffer whereString = new StringBuffer();
		whereString.append("APP_ID =" + app_id );
		whereString.append((" AND " + timeFilter.getEndPropName() + " >=" + timeFilter.getFrom() + " AND " +
				timeFilter.getEndPropName() + "<=" + timeFilter.getTo()));
		if (applicationVersion != null) 
			whereString.append(" AND applicationVersion ='" + applicationVersion + "'");
		if (lrc.getLogLevel() != null)
			whereString.append(" AND logLevel = " + lrc.getLogLevel());
		if (lrc.getLogMessage() != null)
			whereString.append (" AND logMessage like '%" + lrc.getLogMessage() + "%'");

		ogw.whereClause =  whereString.toString();      
		ogw.orderBy = timeFilter.getEndPropName() + " desc";
		ogw.groupBy = "logMessage";     

		return ogw;

	} 

	public static List<Criterion> getRawLogCriteriaList (LogRawCriteria logRawCriteria) {
		LogChartCriteria cq = logRawCriteria.getChartCriteria();
		List<Criterion> filters = new ArrayList<Criterion>();

		//Narrow down by app id first
		if (cq.getAppId() != null)
			filters.add(new AppsFilter(cq.getAppId()).getCriteria());
		//Then by time range
		filters.add( new TimeRangeFilter(cq).getCriteria());		

		if (cq.getDeviceId() != null) 
			filters.add(Restrictions.like("deviceId", cq.getDeviceId().trim(),MatchMode.START));

		if (logRawCriteria.getLogLevel() !=null)
			filters.add( Restrictions.eq("logLevel",logRawCriteria.getLogLevel()));		
		if(logRawCriteria.getTag() != null)
			filters.add(Restrictions.like("tag", logRawCriteria.getTag().trim(),MatchMode.START));
		else {
			if(logRawCriteria.isExcludeCrash())		
			filters.add(Restrictions.ne("tag", "CRASH"));
		}
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

		if (cq.getNetworkCarrier() != null)
			filters.add(Restrictions.like("networkCarrier", cq.getNetworkCarrier().trim(), MatchMode.START));     
		if (cq.getNetworkType() != null)
			filters.add(Restrictions.like("networkType", cq.getNetworkType().trim(), MatchMode.START));
		
		if (logRawCriteria.getLogMessage() != null)
			filters.add(Restrictions.like("logMessage", logRawCriteria.getLogMessage().trim(),MatchMode.ANYWHERE));
		return filters;
	}



}
