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
