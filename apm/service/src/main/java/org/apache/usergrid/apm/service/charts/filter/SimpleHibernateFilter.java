package org.apache.usergrid.apm.service.charts.filter;

import org.hibernate.criterion.Criterion;

public interface SimpleHibernateFilter {
	
	
	
	/**
	 * Check filter state:
	 * <ul>
	 *  <li><b>true</b>: if any of filter's properties are null or empty
	 *  <li><b>false</b>: if at least one filter's properties are non-empty
	 * </ul>
	 */
	public boolean getFilterEmpty();
	
	/**
	 * Prepare Hibernate Criteria according to properties value 
	 * (for empty properties is not necessary add Criterion)
	 * and filter meaning (e.g. date filtering differ from name searching)
	 */
	public Criterion getCriteria();
	
	public void setPropertyName(String propertyName);
	
	public String getPropertyName ();
	
	public Object getPropertyValue ();
	
	

}
