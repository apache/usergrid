package org.apache.usergrid.apm.service;

/**
 * 
 * Service for injesting metrics from the cloud
 * 
 * @author alanho
 *
 */

public interface MetricsInjestionService {
	
	
	public static String VALUE_UNKNOWN = "UNKNOWN";
	
    public void injestMetrics(Long applicationId, String orgAppName);
    
    public void injestAllMetrics();
	
}
