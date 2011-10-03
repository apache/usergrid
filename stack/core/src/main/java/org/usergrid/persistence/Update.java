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

import java.util.ArrayList;
import java.util.List;

public class Update {

	private List<UpdateOperation> operationList = new ArrayList<UpdateOperation>();

	public class UpdateOperation {
		String propertyName;
		UpdateOperator operator;
		Object value;

		UpdateOperation(String propertyName, UpdateOperator operator,
				Object value) {
			this.propertyName = propertyName;
			this.operator = operator;
			this.value = value;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public UpdateOperator getOperator() {
			return operator;
		}

		public Object getValue() {
			return value;
		}
	}

	public static enum UpdateOperator {
		UPDATE, DELETE, ADD_TO_LIST, REMOVE_FROM_LIST;
	}

	public Update() {
	}

	public void add(String propertyName, UpdateOperator operator, Object value) {
		UpdateOperation operation = new UpdateOperation(propertyName, operator,
				value);
		operationList.add(operation);
	}

	public void clear() {
		operationList = new ArrayList<UpdateOperation>();
	}

}
