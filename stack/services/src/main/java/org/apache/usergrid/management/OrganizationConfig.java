/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.management;


import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.usergrid.utils.MapUtils;

import java.util.*;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;


public class OrganizationConfig {

    private Map<String, String> defaultProperties;
    private UUID id;
    private String name;
    private Map<String, String> orgProperties;


    // shouldn't use the default constructor
    private OrganizationConfig() {
    }

    public OrganizationConfig(OrganizationConfigProps configFileProperties, UUID id, String name) {
        defaultProperties = configFileProperties.getPropertyMap();
        this.id = id;
        this.name = name;
        this.orgProperties = new HashMap<>();
    }

    public OrganizationConfig(OrganizationConfigProps configFileProperties) {
        this(configFileProperties, null, null);
    }

    private void orgPropertyValidate(Map<String, Object> entityProperties) throws IllegalArgumentException {
        Set<String> entityPropertyKeys = new HashSet<>(entityProperties.keySet());
        entityPropertyKeys.removeAll(defaultProperties.keySet());
        // if anything remains in the key set, it is not a valid property
        if (entityPropertyKeys.size() > 0) {
            throw new IllegalArgumentException("Invalid organization config keys: " + String.join(", ", entityPropertyKeys));
        }

        entityProperties.forEach((k,v) -> {
            if (!v.getClass().equals(String.class)) {
                throw new IllegalArgumentException("Organization config values must be strings.");
            }
        });
    }

    private void addOrgProperties(Map<String, Object> newOrgProperties) {
        newOrgProperties.forEach((k,v) -> {
            // only take valid properties, validation (if required) happened earlier
            if (defaultProperties.containsKey(k)) {
                // ignore non-strings, validation happened earlier
                if (v.getClass().equals(String.class)) {
                    this.orgProperties.put(k, v.toString());
                }
            }
        });
    }

    public OrganizationConfig(OrganizationConfigProps configFileProperties, UUID id, String name,
                              Map<Object, Object> newOrgProperties, boolean validateOrgProperties)
        throws IllegalArgumentException {
        this(configFileProperties, id, name);

        Map<String, Object> orgPropertiesMap = MapUtils.toStringObjectMap(newOrgProperties);

        // entityPropertyValidate will throw IllegalArgumentException
        if (validateOrgProperties) {
            orgPropertyValidate(orgPropertiesMap);
        }

        addOrgProperties(orgPropertiesMap);
    }

    // adds supplied properties to existing properties
    public void addProperties(Map<String, Object> newOrgProperties, boolean validateOrgProperties)
        throws IllegalArgumentException {

        // entityPropertyValidate will throw IllegalArgumentException if invalid
        if (validateOrgProperties) {
            orgPropertyValidate(newOrgProperties);
        }

        // don't clear properties map -- these overwrite/add to existing
        addOrgProperties(newOrgProperties);
    }

    public Map<String, Object> getOrgConfigCustomMap(Set<String> items, boolean includeDefaults, boolean includeOverrides) {
        Map<String, Object> map = new HashMap<>();

        if (includeDefaults) {
            map.putAll(defaultProperties);
        }

        if (includeOverrides) {
            map.putAll(orgProperties);
        }

        if (items != null) {
            // filter out properties not specified
            map.keySet().retainAll(items);
        }

        return map;
    }

    public Map<String, Object> getOrgConfigMap() {
        return getOrgConfigCustomMap(null, true, true);
    }

    public Map<String, Object> getOrgConfigOverridesMap() {
        return getOrgConfigCustomMap(null, false, true);
    }

    public Map<String, Object> getOrgConfigDefaultsMap() {
        return getOrgConfigCustomMap(null, false, true);
    }

    // only include specified items
    public Map<String, Object> getFilteredOrgConfigMap(Set<String> items) {
        return getOrgConfigCustomMap(items, true, true);
    }

    public Map<String, Object> getFilteredOrgConfigOverridesMap(Set<String> items) {
        return getOrgConfigCustomMap(items, false, true);
    }

    public Map<String, Object> getFilteredOrgConfigDefaultsMap(Set<String> items) {
        return getOrgConfigCustomMap(items, false, true);
    }

    public String getProperty(String key) {
        String retValue = null;
        if (orgProperties != null) {
            retValue = orgProperties.get(key);
        }
        return retValue != null ? retValue : defaultProperties.get(key);
    }

    public String getProperty(String name, String defaultValue) {
        String retValue = getProperty(name);
        return retValue != null ? retValue : defaultValue;
    }

    public boolean isProperty(String name, boolean defaultValue) {
        String val = getProperty(name);
        return isBlank(val) ? defaultValue : Boolean.parseBoolean(val);
    }

    public int intProperty(String name, int defaultValue) {
        String val = getProperty(name);
        return isBlank(val) ? defaultValue : Integer.parseInt(val);
    }

    public long longProperty(String name, long defaultValue) {
        String val = getProperty(name);
        return isBlank(val) ? defaultValue : Long.parseLong(val);
    }

    public UUID getUuid() {
        return id;
    }

    public void setUuid( UUID id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDefaultConnectionParam() {
        return getProperty(OrganizationConfigProps.PROPERTIES_DEFAULT_CONNECTION_PARAM);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        OrganizationConfig other = (OrganizationConfig) obj;
        if ( id == null ) {
            if ( other.id != null ) {
                return false;
            }
        }
        else if ( !id.equals( other.id ) ) {
            return false;
        }
        if ( name == null ) {
            if ( other.name != null ) {
                return false;
            }
        }
        else if ( !name.equals( other.name ) ) {
            return false;
        }
        return getOrgConfigMap().equals(other.getOrgConfigMap());
    }

}
