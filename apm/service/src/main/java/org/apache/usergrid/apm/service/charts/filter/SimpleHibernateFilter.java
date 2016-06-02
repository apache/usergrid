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
