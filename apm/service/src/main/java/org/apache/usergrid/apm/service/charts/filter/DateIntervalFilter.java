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
package org.apache.usergrid.apm.service.charts.filter;

import java.util.Date;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class DateIntervalFilter implements SimpleHibernateFilter {

	private String startTime = "startTime";
	private String endTime = "endTime";
	
	private Date from;
	private Date to;
	
	/**
	 * 
	 * @param propertyName - Bean's property name used as filter property
	 */
	public DateIntervalFilter(Date from, Date to) {		
		this.from = from;
		this.to = to;
	}
	
	/**
	 * Check if filter should be considered as empty 
	 * and should be ignored when create Criterion
	 * @return isEmpty: 
	 *  <b>true</b> if all filter's properties are null
	 *  <b>false</b> if at least one property is non-null
	 */
	public boolean getFilterEmpty() {
		return from == null && to == null;
	}
	
	/** 
	 * Prepare criteria according to filter values. Ideally when both from and to are present, it should result into two criterion
	 * startTime >= from and endTime <= to but given that delta between startTime and endTime is not huge and we don't have to be 
	 * precise upto few minutes, selecting startTime between from and to should work. It should also help with faster query as well.
	 * @return
	 */
	public Criterion getCriteria() {
		
		if (getFilterEmpty()) 
			return null;
		
		if (from != null && to != null ) 
			return Restrictions.between(startTime, from, to);		
		
		
		Criterion crit = null;
		if (from != null)
			crit = Restrictions.gt(startTime, from);		
		else				
			crit = Restrictions.lt(endTime, to);
		
		return crit;
	}
	
	public Date getFrom() {
		return from;
	}
	public void setFrom(Date from) {
		this.from = from;
	}
	public Date getTo() {
		return to;
	}
	public void setTo(Date to) {
		this.to = to;
	}

	public String getPropertyName() {
		throw new UnsupportedOperationException ();
	}

	public void setPropertyName(String propertyName) {
		throw new UnsupportedOperationException ();
	}

	
	public Object getPropertyValue() {
		throw new UnsupportedOperationException ();
	}
	
	
}
	
