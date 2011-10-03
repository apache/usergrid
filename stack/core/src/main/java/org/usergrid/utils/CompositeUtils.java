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
package org.usergrid.utils;

import java.nio.ByteBuffer;
import java.util.List;

import me.prettyprint.hector.api.beans.AbstractComposite.Component;
import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.DynamicComposite;

public class CompositeUtils {

	public static Object deserialize(ByteBuffer bytes) {
		List<Object> objects = DynamicComposite.fromByteBuffer(bytes);
		if (objects.size() > 0) {
			return objects.get(0);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static DynamicComposite setEqualityFlag(DynamicComposite composite,
			ComponentEquality eq) {
		if (composite.isEmpty()) {
			return composite;
		}
		int i = composite.size() - 1;
		@SuppressWarnings("rawtypes")
		Component c = composite.getComponent(i);
		composite.setComponent(i, c.getValue(), c.getSerializer(),
				c.getComparator(), eq);
		return composite;
	}

	public static DynamicComposite setGreaterThanEqualityFlag(
			DynamicComposite composite) {
		return setEqualityFlag(composite, ComponentEquality.GREATER_THAN_EQUAL);
	}

}
