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
import org.hibernate.criterion.Restrictions;

import org.apache.usergrid.apm.service.charts.filter.AppsFilter;
import org.apache.usergrid.apm.service.charts.filter.TimeRangeFilter;
import org.apache.usergrid.apm.model.LogChartCriteria;


public class CrashChartUtil {

	private static final Log log = LogFactory.getLog(CrashChartUtil.class);




	public static List<Criterion> getRawCrashCriteriaList (CrashRawCriteria crashCriteria) {
		LogChartCriteria cq = crashCriteria.getChartCriteria();
		List<Criterion> filters = new ArrayList<Criterion>();

		//Narrow down by app id first
		if (cq.getAppId() != null)
			filters.add(new AppsFilter(cq.getAppId()).getCriteria());
		//Then by time range
		filters.add( new TimeRangeFilter(cq).getCriteria());	

		if (cq.getDeviceId() != null) 
			filters.add(Restrictions.like("deviceId", cq.getDeviceId().trim(),MatchMode.START));
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
		if (crashCriteria.getCrashSummary() != null)
			filters.add(Restrictions.like("crashSummary", crashCriteria.getCrashSummary().trim(), MatchMode.ANYWHERE));
		return filters;
	}

}
