package org.apache.usergrid.apm.service;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;

import org.apache.usergrid.apm.model.ClientNetworkMetrics;
import org.apache.usergrid.apm.service.charts.service.AggregatedNetworkData;
import org.apache.usergrid.apm.service.charts.service.NetworkMetricsRawCriteria;
import org.apache.usergrid.apm.model.CompactNetworkMetrics;
import org.apache.usergrid.apm.model.MetricsChartCriteria;



/**
 * Service for performing Database operations.
 *
 * @author prabhat jha prabhat143@gmail.com
 */
public interface NetworkMetricsDBService {

    /**
     * 
     * @param pcb
     * @throws HibernateException
     */
	public void saveNetworkMetrics(ClientNetworkMetrics wsRecord) throws HibernateException;

	/**
     * 
     * @param pcb
     * @throws HibernateException
     */
	public void saveNetworkMetricsInBatch(List<ClientNetworkMetrics> wsRecords) throws HibernateException;
	
	
	/**
	 * For some weird reason you may want to get all the rows from DB. Make sure you have big enough heap size
	 * @return
	 * @throws HibernateException
	 */
    
    public List<ClientNetworkMetrics> getAllNetworkMetrics() throws HibernateException;
    
    
     /**
	 * @param startId  if null, first result criterion is not set
	 * @param numberOfRows if 0, maxResult criteria is not set
	 * @param criteria	
     * @param order
     * @return
     * @throws HibernateException
     */
    
    public List<ClientNetworkMetrics> getNetworkMetricsList(Integer startId, int numberOfRows, List<Criterion> criteria, List<Order> orders) throws HibernateException;
    
    public List<ClientNetworkMetrics> getNetworkMetricsForApp(Long appId) throws HibernateException;
    
    /**
     * Return type is {@link CompactNetworkMetrics} instead of {@link ClientNetworkMetrics} because once we get the raw data
     * we persist those in cache table i.e {@link CompactNetworkMetrics} . We do hibernate transformation to do the conversion
     * @param criteria
     * @param projections
     * @param orders
     * @return
     * @throws HibernateException
     */
   // public List<CompactNetworkMetrics> getHourlyRawMetrics(List<Criterion> criteria, ProjectionList projections, List<Order> orders) throws HibernateException;
    
    public List<CompactNetworkMetrics> getCompactNetworkMetrics(List<Criterion> criteria, List<Order> orders) throws HibernateException;
    
    public void saveCompactNetworkMetrics(CompactNetworkMetrics obj) throws HibernateException;
    
    public void saveCompactNetworkMetricsInBatch(List<CompactNetworkMetrics> objs) throws HibernateException;
    
    public List<String> getUniqueUrls(List<Criterion> criteria) throws HibernateException ;  
    
    public AggregatedNetworkData getAggregatedNetworkMetricsData (MetricsChartCriteria chartCriteria) throws HibernateException;
    
    public List<ClientNetworkMetrics> getRawNetworkMetricsData(NetworkMetricsRawCriteria nmRawCriteria) throws HibernateException;
    public List<ClientNetworkMetrics> getRawNetworkMetricsDataForAPointInTime(Long appId, Long endMinute, int maxResults) throws HibernateException;
    
    /**
     * This is mostly for pagination where you need to know total numbe of page sizes. Once you know that then you would use
     * @link {@link #getRawNetworkMetricsData(NetworkMetricsRawCriteria) to get actual data to paginate
     * @param nmRawCriteria
     * @return
     * @throws HibernateException
     */
    public int getRawNetworkMetricsDataCount (NetworkMetricsRawCriteria nmRawCriteria) throws HibernateException;
    
    public int getRowCount();
    
    public List<CompactNetworkMetrics> getCompactNetworkMetricsUsingNativeSqlQuery(MetricsChartCriteria cq) throws HibernateException;
}
