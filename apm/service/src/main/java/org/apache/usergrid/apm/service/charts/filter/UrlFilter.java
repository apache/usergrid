package org.apache.usergrid.apm.service.charts.filter;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

public class UrlFilter implements SimpleHibernateFilter {
	
	private String propertyName = "url";
	private String urlValue;
	
	public UrlFilter() {
		
	}
	
	public UrlFilter ( String urlRegex) {		
		this.urlValue = urlRegex;
	}

	@Override
	public Criterion getCriteria() {
		if (getFilterEmpty()) 
			return null;
		
		return Restrictions.like(propertyName, urlValue, MatchMode.ANYWHERE);
	}

	@Override
	public boolean getFilterEmpty() {
		return urlValue == null || "".equals(urlValue);
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}


	public String getUrlValue() {
		return urlValue;
	}

	public void setUrlValue(String urlValue) {
		this.urlValue = urlValue;
	}

	@Override
	public Object getPropertyValue() {
		return urlValue;
	}	

}
