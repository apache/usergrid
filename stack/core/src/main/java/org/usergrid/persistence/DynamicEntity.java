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
package org.usergrid.persistence;

import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.usergrid.persistence.annotations.EntityProperty;
import org.usergrid.utils.UUIDUtils;

/**
 * Dynamic entities can represent any entity type whether specified in the
 * Schema or not.
 * 
 * @author edanuff
 * 
 */
@XmlRootElement
public class DynamicEntity extends AbstractEntity {

	protected String type;

	/**
	 * 
	 */
	public DynamicEntity() {
		// setId(UUIDUtils.newTimeUUID());
	}

	/**
	 * @param id
	 */
	public DynamicEntity(UUID id) {
		setUuid(id);
	}

	/**
	 * @param type
	 */
	public DynamicEntity(String type) {
		setUuid(UUIDUtils.newTimeUUID());
		setType(type);
	}

	/**
	 * @param id
	 * @param type
	 */
	public DynamicEntity(String type, UUID id) {
		setUuid(id);
		setType(type);
	}

	/**
	 * @param id
	 * @param type
	 */
	public DynamicEntity(String type, UUID id, Map<String, Object> propertyMap) {
		setUuid(id);
		setType(type);
		setProperties(propertyMap);
	}

	@Override
	@EntityProperty(required = true, mutable = false, basic = true)
	public String getType() {
		return type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

}
