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

import org.usergrid.persistence.annotations.EntityProperty;

public class PropertyInfo {
	private String name;
	private Class<?> type;

	private boolean aliasProperty;
	private boolean fulltextIndexed;
	private boolean indexed;
	private boolean indexedInConnections;
	private boolean basic = false;
	private boolean mutable = true;
	private boolean pathBasedName;
	private boolean publicVisible = true;
	private boolean required;
	private boolean unique;
	private boolean includedInExport = true;
	private boolean timestamp = false;

	public PropertyInfo() {
	}

	public PropertyInfo(EntityProperty propertyAnnotation) {
		setName(propertyAnnotation.name());
		setAliasProperty(propertyAnnotation.aliasProperty());
		setFulltextIndexed(propertyAnnotation.fulltextIndexed());
		setIndexed(propertyAnnotation.indexed());
		setIndexedInConnections(propertyAnnotation.indexedInConnections());
		setBasic(propertyAnnotation.basic());
		setMutable(propertyAnnotation.mutable());
		setPathBasedName(propertyAnnotation.pathBasedName());
		setPublic(propertyAnnotation.publicVisible());
		setRequired(propertyAnnotation.required());
		setUnique(propertyAnnotation.unique());
		setIncludedInExport(propertyAnnotation.includedInExport());
		setTimestamp(propertyAnnotation.timestamp());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if ("".equals(name)) {
			name = null;
		}
		this.name = name;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		if (type == Object.class) {
			type = null;
		}
		this.type = type;
	}

	public boolean isIndexed() {
		return indexed;
	}

	public void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}

	public boolean isBasic() {
		return basic;
	}

	public void setBasic(boolean basic) {
		this.basic = basic;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isMutable() {
		return mutable;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public boolean isIndexedInConnections() {
		return indexedInConnections;
	}

	public void setIndexedInConnections(boolean indexedInConnections) {
		this.indexedInConnections = indexedInConnections;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isAliasProperty() {
		return aliasProperty;
	}

	public void setAliasProperty(boolean aliasProperty) {
		this.aliasProperty = aliasProperty;
		if (aliasProperty) {
			mutable = false;
		}
	}

	public boolean isPathBasedName() {
		return pathBasedName;
	}

	public void setPathBasedName(boolean pathBasedName) {
		this.pathBasedName = pathBasedName;
	}

	public boolean isFulltextIndexed() {
		return fulltextIndexed;
	}

	public void setFulltextIndexed(boolean fulltextIndexed) {
		this.fulltextIndexed = fulltextIndexed;
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

	public boolean isTimestamp() {
		return timestamp;
	}

	public void setTimestamp(boolean timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "PropertyInfo [name=" + name + ", type=" + type
				+ ", aliasProperty=" + aliasProperty + ", fulltextIndexed="
				+ fulltextIndexed + ", indexed=" + indexed
				+ ", indexedInConnections=" + indexedInConnections + ", basic="
				+ basic + ", mutable=" + mutable + ", pathBasedName="
				+ pathBasedName + ", publicVisible=" + publicVisible
				+ ", required=" + required + ", unique=" + unique
				+ ", includedInExport=" + includedInExport + ", timestamp="
				+ timestamp + "]";
	}

}
