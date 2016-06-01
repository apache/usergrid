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
package org.apache.usergrid.apm.model;

import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.Table;


@Entity
@Table(name = "METRICS_CHART_CRITERIA")
@org.hibernate.annotations.Table(appliesTo="METRICS_CHART_CRITERIA",
indexes = {
@Index(name="NetworkMetricsChartCriteriaByApp", columnNames={"appId"} ) 
} )
public class MetricsChartCriteria extends ChartCriteria {

	private String url;

	private Long lowerLatency;

	private Long upplerLatency;

	private Long lowerErrorCount;

	private Long upperErrorCount;

	private boolean errored;

	private boolean groupedByDomain;

	boolean showError;
	boolean showLatency;
	boolean showSamples;
	boolean showAll;

	/**
	 * When this flag is turned on, only metrics from cached table such as CompactNetworkMetrics will be returend
	 */
	boolean cacheDataOnly;

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public Long getLowerLatency()
	{
		return lowerLatency;
	}

	public void setLowerLatency(Long lowerLatency)
	{
		this.lowerLatency = lowerLatency;
	}

	public Long getUpplerLatency()
	{
		return upplerLatency;
	}

	public void setUpplerLatency(Long upplerLatency)
	{
		this.upplerLatency = upplerLatency;
	}

	public Long getLowerErrorCount()
	{
		return lowerErrorCount;
	}

	public void setLowerErrorCount(Long lowerErrorCount)
	{
		this.lowerErrorCount = lowerErrorCount;
	}

	public Long getUpperErrorCount()
	{
		return upperErrorCount;
	}

	public void setUpperErrorCount(Long upperErrorCount)
	{
		this.upperErrorCount = upperErrorCount;
	}

	public boolean isErrored()
	{
		return errored;
	}

	public void setErrored(boolean errored)
	{
		this.errored = errored;
	}

	public boolean isShowError()
	{
		return showError;
	}

	public void setShowError(boolean showError)
	{
		this.showError = showError;
	}

	public boolean isShowLatency()
	{
		return showLatency;
	}

	public void setShowLatency(boolean showLatency)
	{
		this.showLatency = showLatency;
	}

	public boolean isShowSamples()
	{
		return showSamples;
	}

	public void setShowSamples(boolean showSamples)
	{
		this.showSamples = showSamples;
	}

	public boolean isShowAll()
	{
		return showAll;
	}

	public void setShowAll(boolean showAll)
	{
		if (showAll) {
			this.showError = true;
			this.showLatency = true;
			this.showSamples = true;
		}
		this.showAll = showAll;
	}

	public boolean isCacheDataOnly()
	{
		return cacheDataOnly;
	}

	public void setCacheDataOnly(boolean cacheDataOnly)
	{
		this.cacheDataOnly = cacheDataOnly;
	}

	public boolean isGroupedByDomain() {
		return groupedByDomain;
	}

	public void setGroupedByDomain(boolean groupedByDomain) {
		this.groupedByDomain = groupedByDomain;
	}
	
	@Override
	public boolean hasGrouping () {
		return super.hasGrouping() || groupedByDomain;
	}



}
