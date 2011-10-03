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

import static org.usergrid.persistence.cassandra.IndexUpdate.compareIndexedValues;

import java.util.Comparator;

public class EntityPropertyComparator implements Comparator<Entity> {

	final boolean reverse;
	final String propertyName;

	public EntityPropertyComparator(String propertyName) {
		this.propertyName = propertyName;
		reverse = false;
	}

	public EntityPropertyComparator(String propertyName, boolean reverse) {
		this.propertyName = propertyName;
		this.reverse = reverse;
	}

	@Override
	public int compare(Entity e1, Entity e2) {

		if ((e1 == null) && (e2 == null)) {
			return 0;
		} else if (e1 == null) {
			return -1;
		} else if (e2 == null) {
			return 1;
		}

		int c = compareIndexedValues(e1.getProperty(propertyName),
				e2.getProperty(propertyName));
		if (reverse) {
			c = -c;
		}
		return c;
	}

}
