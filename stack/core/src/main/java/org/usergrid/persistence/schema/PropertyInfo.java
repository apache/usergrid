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
