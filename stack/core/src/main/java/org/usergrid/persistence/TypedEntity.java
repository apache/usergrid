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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * TypedEntity is the abstract superclass for all typed entities. A typed entity
 * refers to an entity that has a concrete Java class mapped to it. Entities do
 * not need to have concrete typed classes, the service interacts with entities
 * in an entirely dynamic fashion and uses the Schema class to determine
 * relationships and property types, however using the typed entity classes can
 * be more convenient.
 * 
 * @author edanuff
 */
@XmlRootElement
public abstract class TypedEntity extends AbstractEntity {

	@Override
	public Entity toTypedEntity() {
		return this;
	}
}
