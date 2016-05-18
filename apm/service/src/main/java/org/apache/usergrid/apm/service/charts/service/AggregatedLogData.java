package org.apache.usergrid.apm.service.charts.service;

public class AggregatedLogData implements AggregatedData
{



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long totalAsserts;
	private Long totalErrors;
	private Long totalWarnings;
	private Long totalEvents;
	private Long totalCrashes;
	private Long totalErrorsAndAbove;
	/**
	 * totalErrosAndAbove-totalCrashes
	 * logLevel with Error and log level with Assert - crashCount. This is needed for ErrorsOnly UI page.
	 */
	private Long totalAppErrors;
	
	/**
	 * totalAsserts - totalCrashes
	 */
	private Long totalCriticalErrors;


	public Long getTotalAsserts() {
		return totalAsserts;
	}

	public void setTotalAsserts(Long totalAsserts) {
		this.totalAsserts = totalAsserts;
	}

	public Long getTotalErrors()
	{
		return totalErrors;
	}

	public void setTotalErrors(Long totalErrors)
	{
		this.totalErrors = totalErrors;
	}

	public Long getTotalWarnings()
	{
		return totalWarnings;
	}

	public void setTotalWarnings(Long totalWarnings)
	{
		this.totalWarnings = totalWarnings;
	}

	public Long getTotalEvents()
	{
		return totalEvents;
	}

	public void setTotalEvents(Long totalEvents)
	{
		this.totalEvents = totalEvents;
	}
	

	public Long getTotalCrashes() {
		return totalCrashes;
	}

	public void setTotalCrashes(Long totalCrashes) {
		this.totalCrashes = totalCrashes;
	}	

	public Long getTotalErrorsAndAbove() {
		return totalErrorsAndAbove;
	}

	public void setTotalErrorsAndAbove(Long totalErrorsAndAbove) {
		this.totalErrorsAndAbove = totalErrorsAndAbove;
	}	

	public Long getTotalAppErrors() {
		return totalAppErrors;
	}

	public void setTotalAppErrors(Long totalAppErrors) {
		this.totalAppErrors = totalAppErrors;
	}	

	public Long getTotalCriticalErrors() {
		return totalCriticalErrors;
	}

	public void setTotalCriticalErrors(Long totalCriticalErrors) {
		this.totalCriticalErrors = totalCriticalErrors;
	}

	@Override
	public Long getTotalMetricsCount() {
		return totalEvents;
	}


}
