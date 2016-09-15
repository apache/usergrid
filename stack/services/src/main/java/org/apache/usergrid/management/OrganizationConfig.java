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


import org.apache.usergrid.management.cassandra.OrganizationConfigPropsImpl;
import org.apache.usergrid.management.OrganizationConfigProps.*;
import org.apache.usergrid.utils.MapUtils;

import java.util.*;



public class OrganizationConfig {

    private OrganizationConfigProps configProps;
    private UUID id;
    private String name;


    // shouldn't use the default constructor
    private OrganizationConfig() {
    }

    public OrganizationConfig(OrganizationConfig orgConfig) {
        this.id = orgConfig.getUuid();
        this.name = orgConfig.getName();
        this.configProps = orgConfig.getOrgConfigProps();
    }

    public OrganizationConfig(Properties properties) {
        this.configProps = new OrganizationConfigPropsImpl(properties);
        this.id = null;
        this.name = null;
    }

    public OrganizationConfig(OrganizationConfigProps configFileProperties, UUID id, String name) {
        this.configProps = new OrganizationConfigPropsImpl(configFileProperties);
        this.id = id;
        this.name = name;
    }

    public OrganizationConfig(OrganizationConfigProps configFileProperties) {
        this(configFileProperties, null, null);
    }

    public OrganizationConfig(OrganizationConfigProps configFileProperties, UUID id, String name,
                              Map<Object, Object> newOrgProperties, boolean validateOrgProperties)
            throws IllegalArgumentException {
        this(configFileProperties, id, name);

        Map<String, Object> orgPropertiesMap = MapUtils.toStringObjectMap(newOrgProperties);

        // orgPropertyValidate will throw IllegalArgumentException
        if (validateOrgProperties) {
            orgPropertyValidate(orgPropertiesMap);
        }

        addOrgProperties(orgPropertiesMap);
    }

    private void orgPropertyValidate(Map<String, Object> entityProperties) throws IllegalArgumentException {
        Set<String> invalidKeys = new HashSet<>();
        entityProperties.keySet().forEach((k) -> {
           if (!configProps.orgPropertyNameValid(k)) {
               invalidKeys.add(k);
           }
        });

        if (invalidKeys.size() > 0) {
            throw new IllegalArgumentException("Invalid organization config keys: " + String.join(", ", invalidKeys));
        }

        invalidKeys.clear();
        entityProperties.forEach((k,v) -> {
            if (!v.getClass().equals(String.class)) {
                invalidKeys.add(k);
            }
        });

        if (invalidKeys.size() > 0) {
            throw new IllegalArgumentException("Organization config value(s) not strings: " + String.join(", ", invalidKeys));
        }
    }

    private void addOrgProperties(Map<String, Object> newOrgProperties) {
        newOrgProperties.forEach((k,v) -> {
            // only take valid properties, validation (if required) happened earlier
            if (configProps.orgPropertyNameValid(k)) {
                // ignore non-strings, validation happened earlier
                if (v.getClass().equals(String.class)) {
                    this.configProps.setProperty(k, v.toString());
                }
            }
        });
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
            map.putAll(configProps.getDefaultPropertiesMap());
        }

        if (includeOverrides) {
            map.putAll(configProps.getOrgPropertiesMap());
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
        return configProps.getProperty(key);
    }

    public String getProperty(String name, String defaultValue) {
        return configProps.getProperty(name, defaultValue);
    }

    public boolean boolProperty(String name, boolean defaultValue) {
        return configProps.boolProperty(name, defaultValue);
    }

    public int intProperty(String name, int defaultValue) {
        return configProps.intProperty(name, defaultValue);
    }

    public long longProperty(String name, long defaultValue) {
        return configProps.longProperty(name, defaultValue);
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

    protected OrganizationConfigProps getOrgConfigProps() {
        return new OrganizationConfigPropsImpl(configProps);
    }

    public String getFullUrlTemplate(WorkflowUrl urlType) {
        return configProps.getFullUrlTemplate(urlType);
    }

    public String getFullUrl(WorkflowUrl urlType, Object ... arguments) {
        return configProps.getFullUrl(urlType, arguments);
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
