/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
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
