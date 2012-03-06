/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.exceptions;

public class PropertyTypeConversionException extends PersistenceException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final String entityType;
	final String propertyName;
	final Object propertyValue;
	final Class<?> propertyType;

	public PropertyTypeConversionException(String entityType,
			String propertyName, Object propertyValue, Class<?> propertyType) {
		super("Unable to convert property \"" + propertyName
				+ "\" of entity \"" + entityType + "\" from value of type "
				+ propertyValue.getClass() + " to value of type "
				+ propertyType);
		this.entityType = entityType;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.propertyType = propertyType;
	}

	public PropertyTypeConversionException(String entityType,
			String propertyName, Object propertyValue, Class<?> propertyType,
			Throwable cause) {
		super("Unable to convert property \"" + propertyName
				+ "\" of entity \"" + entityType + "\" from value of type "
				+ propertyValue.getClass() + " to value of type "
				+ propertyType, cause);
		this.entityType = entityType;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.propertyType = propertyType;
	}

	public String getEntityType() {
		return entityType;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Object getPropertyValue() {
		return propertyValue;
	}

	public Class<?> getPropertyType() {
		return propertyType;
	}
}
