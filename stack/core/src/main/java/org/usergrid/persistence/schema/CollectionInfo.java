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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.usergrid.persistence.Schema;
import org.usergrid.persistence.annotations.EntityCollection;

public class CollectionInfo {

	private String name;
	private EntityInfo container;

	private boolean indexingDynamicDictionaries;
	private String linkedCollection;
	private Set<String> propertiesIndexed = null;
	private boolean publicVisible = true;
	private final Set<String> dictionariesIndexed = new TreeSet<String>(
			String.CASE_INSENSITIVE_ORDER);
	private Set<String> subkeys = new LinkedHashSet<String>();
	private String type;
	private boolean reversed;
	private boolean includedInExport = true;
	private String sort;

	public CollectionInfo() {
	}

	public CollectionInfo(EntityCollection collectionAnnotation) {
		setIndexingDynamicDictionaries(collectionAnnotation
				.indexingDynamicDictionaries());
		setLinkedCollection(collectionAnnotation.linkedCollection());
		setPublic(collectionAnnotation.publicVisible());
		setDictionariesIndexed(new LinkedHashSet<String>(
				Arrays.asList(collectionAnnotation.dictionariesIndexed())));
		setSubkeys(Arrays.asList(collectionAnnotation.subkeys()));
		setType(collectionAnnotation.type());
		setReversed(collectionAnnotation.reversed());
		setIncludedInExport(collectionAnnotation.includedInExport());
		setSort(collectionAnnotation.sort());
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		if ("".equals(type)) {
			type = null;
		}
		this.type = type;
	}

	public boolean isPropertyIndexed(String propertyName) {
		return getPropertiesIndexed().contains(propertyName);
	}

	public boolean hasIndexedProperties() {
    return !getPropertiesIndexed().isEmpty();
	}

	public Set<String> getPropertiesIndexed() {
    if (propertiesIndexed != null) return propertiesIndexed;
    return Schema.getDefaultSchema().getEntityInfo(getType()).getIndexedProperties();
	}

	public void setPropertiesIndexed(Set<String> propertiesIndexed) {
		this.propertiesIndexed = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		this.propertiesIndexed.addAll(propertiesIndexed);
	}

	public boolean isDictionaryIndexed(String propertyName) {
		return dictionariesIndexed.contains(propertyName);
	}

	public Set<String> getDictionariesIndexed() {
		return dictionariesIndexed;
	}

	public void setDictionariesIndexed(Set<String> dictionariesIndexed) {
		dictionariesIndexed = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		dictionariesIndexed.addAll(dictionariesIndexed);
	}

	public boolean isIndexingDynamicDictionaries() {
		return indexingDynamicDictionaries;
	}

	public void setIndexingDynamicDictionaries(
			boolean indexingDynamicDictionaries) {
		this.indexingDynamicDictionaries = indexingDynamicDictionaries;
	}

	public String getLinkedCollection() {
		return linkedCollection;
	}

	public void setLinkedCollection(String linkedCollection) {
		if ("".equals(linkedCollection)) {
			linkedCollection = null;
		}
		this.linkedCollection = linkedCollection;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EntityInfo getContainer() {
		return container;
	}

	public void setContainer(EntityInfo entityInfo) {
		container = entityInfo;
	}

	public boolean isSubkeyProperty(String propertyName) {
		return subkeys.contains(propertyName);
	}

	public boolean hasSubkeys() {
		return !subkeys.isEmpty();
	}

	public Set<String> getSubkeySet() {
		return subkeys;
	}

	public List<String> getSubkeys() {
		return new ArrayList<String>(subkeys);
	}

	public void setSubkeys(List<String> s) {
		subkeys = new LinkedHashSet<String>();
		subkeys.addAll(s);
		makeSubkeyCombos();
	}

	List<String[]> subkeyCombinations = new ArrayList<String[]>();

	void makeSubkeyCombos() {
		subkeyCombinations = new ArrayList<String[]>();

		if (subkeys.size() > 0) {
			int combos = (1 << subkeys.size());
			// System.out.println(subkeys.size() + " elements = " +
			// combos
			// + " combos");
			for (int i = 1; i < combos; i++) {
				List<String> combo = new ArrayList<String>();
				int j = 0;
				for (String subkey : subkeys) {
					if (((1 << j) & i) != 0) {
						combo.add(subkey);
					} else {
						combo.add(null);
					}
					j++;
				}
				subkeyCombinations.add(combo.toArray(new String[0]));
			}
		}
	}

	public List<String[]> getSubkeyCombinations() {
		return subkeyCombinations;
	}

	public boolean isPublic() {
		return publicVisible;
	}

	public void setPublic(boolean publicVisible) {
		this.publicVisible = publicVisible;
	}

	public boolean isReversed() {
		return reversed;
	}

	public void setReversed(boolean reversed) {
		this.reversed = reversed;
	}

	public void setIncludedInExport(boolean includedInExport) {
		this.includedInExport = includedInExport;
	}

	public boolean isIncludedInExport() {
		return includedInExport;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		if ("".equals(sort)) {
			sort = null;
		}
		this.sort = sort;
	}

	@Override
	public String toString() {
		return "CollectionInfo [name=" + name
				+ ", indexingDynamicDictionaries="
				+ indexingDynamicDictionaries + ", linkedCollection="
				+ linkedCollection + ", propertiesIndexed=" + propertiesIndexed
				+ ", publicVisible=" + publicVisible + ", dictionariesIndexed="
				+ dictionariesIndexed + ", subkeys=" + subkeys + ", type="
				+ type + ", reversed=" + reversed + ", includedInExport="
				+ includedInExport + ", sort=" + sort + ", subkeyCombinations="
				+ subkeyCombinations + "]";
	}

}
