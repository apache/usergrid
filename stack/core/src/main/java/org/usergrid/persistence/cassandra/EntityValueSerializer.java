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
package org.usergrid.persistence.cassandra;

import static org.usergrid.utils.ConversionUtils.bytebuffer;

import java.nio.ByteBuffer;
import java.util.List;

import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.hector.api.beans.DynamicComposite;

public class EntityValueSerializer extends AbstractSerializer<Object> {

	@Override
	public ByteBuffer toByteBuffer(Object obj) {
		ByteBuffer bytes = null;
		if (obj instanceof List) {
			bytes = DynamicComposite.toByteBuffer((List<?>) obj);
		} else if (obj instanceof Object[]) {
			bytes = DynamicComposite.toByteBuffer((Object[]) obj);
		} else {
			bytes = bytebuffer(obj);
		}
		return bytes;
	}

	@Override
	public Object fromByteBuffer(ByteBuffer byteBuffer) {
		throw new IllegalStateException(
				"The entity value serializer can only be used for data going to the database, and not data coming from the database");
	}

}
