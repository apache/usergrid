package org.apache.usergrid.apm.service.charts.service;

import org.apache.usergrid.apm.model.MetricsChartCriteria;

public class NetworkMetricsRawCriteria extends RawCriteria<MetricsChartCriteria>
{

	/**
    Let's use the fields in MetricsChartCriteria. If it gets confusing we will use following

   private String urlFilter;

   private String platformFilter;

   private String networkCarrierFilter;

   private String newtowrkTypeFilter;

	 **/

	private Long httpStatusCode;

	public NetworkMetricsRawCriteria(MetricsChartCriteria cq)
	{
		super(cq);
		// TODO Auto-generated constructor stub
	}

	public Long getHttpStatusCode() {
		return httpStatusCode;
	}

	public void setHttpStatusCode(Long httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}



}
