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
