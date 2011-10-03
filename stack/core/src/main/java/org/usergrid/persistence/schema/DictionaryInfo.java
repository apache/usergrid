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
package org.usergrid.persistence.schema;

import org.usergrid.persistence.annotations.EntityDictionary;

public class DictionaryInfo {
	private String name;
	private Class<?> keyType;

	private Class<?> valueType; // = Long.class.getName();
	private boolean keysIndexedInConnections;
	private boolean publicVisible = true;
	private boolean includedInExport = true;;

	public DictionaryInfo() {
	}

	public DictionaryInfo(EntityDictionary setAnnotation) {
		setKeyType(setAnnotation.keyType());
		setValueType(setAnnotation.valueType());
		setKeysIndexedInConnections(setAnnotation.keysIndexedInConnections());
		setPublic(setAnnotation.publicVisible());
		setIncludedInExport(setAnnotation.includedInExport());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getKeyType() {
		return keyType;
	}

	public void setKeyType(Class<?> type) {
		if (type == Object.class) {
			type = null;
		}
		keyType = type;
	}

	public Class<?> getValueType() {
		return valueType;
	}

	public void setValueType(Class<?> valueType) {
		if (valueType == Object.class) {
			valueType = null;
		}
		this.valueType = valueType;
	}

	public boolean isKeysIndexedInConnections() {
		return keysIndexedInConnections;
	}

	public void setKeysIndexedInConnections(boolean keysIndexedInConnections) {
		this.keysIndexedInConnections = keysIndexedInConnections;
	}

	public boolean isPublic() {
		return publicVisible;
	}

	public void setPublic(boolean publicVisible) {
		this.publicVisible = publicVisible;
	}

	public boolean isIncludedInExport() {
		return includedInExport;
	}

	public void setIncludedInExport(boolean includedInExport) {
		this.includedInExport = includedInExport;
	}

	@Override
	public String toString() {
		return "Set [name=" + name + ", keyType=" + keyType + ", valueType="
				+ valueType + ", keysIndexedInConnections="
				+ keysIndexedInConnections + ", publicVisible=" + publicVisible
				+ "]";
	}

}
