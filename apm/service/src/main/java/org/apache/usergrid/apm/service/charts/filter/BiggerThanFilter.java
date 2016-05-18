package org.apache.usergrid.apm.service.charts.filter;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public abstract class BiggerThanFilter implements SimpleHibernateFilter {
	protected String propertyName;
	protected Object propertyValue;

	
	public Criterion getCriteria() {
		if (getFilterEmpty()) 
			return null;
		
		return Restrictions.gt("this."+propertyName, propertyValue);
	}
	
	public boolean getFilterEmpty() {
		return propertyValue == null;
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
	
	@Override
	public Object getPropertyValue ()  {
		return propertyValue;
	}

}
