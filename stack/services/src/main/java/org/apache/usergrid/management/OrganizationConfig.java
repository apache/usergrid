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


import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.persistence.Schema.PROPERTY_PATH;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;


public class OrganizationConfig {

    public static final String DEFAULT_CONNECTION_PARAM_PROPERTY = "defaultConnectionParam";
    private static final String DEFAULT_CONNECTION_PARAM_DEFAULT_VALUE = "all";

    private static final String [] propertyNames = {
            DEFAULT_CONNECTION_PARAM_PROPERTY
    };

    private static final String [] defaultValues = {
            DEFAULT_CONNECTION_PARAM_DEFAULT_VALUE
    };

    private UUID id;
    private String name;
    private Map<String, Object> properties;


    public OrganizationConfig() {
    }


    public OrganizationConfig(UUID id, String name) {
        this.id = id;
        this.name = name;
    }


    public OrganizationConfig(Map<String, Object> properties) {
        id = ( UUID ) properties.get( PROPERTY_UUID );
        name = ( String ) properties.get( PROPERTY_PATH );
    }


    public OrganizationConfig(UUID id, String name, Map<String, Object> properties) {
        this( id, name );
        this.properties = properties;

        // add default values to properties map
        addDefaultsToProperties();
    }

    private void addDefaultsToProperties()  {
        for (int i=0; i < propertyNames.length; i++) {
            if (!properties.containsKey(propertyNames[i])) {
                properties.put(propertyNames[i], defaultValues[i]);
            }
        }
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
        String defaultParam = DEFAULT_CONNECTION_PARAM_DEFAULT_VALUE;
        if ( properties != null ) {
            Object paramValue = properties.get( DEFAULT_CONNECTION_PARAM_PROPERTY );
            if ( paramValue instanceof String ) {
                defaultParam = ( String ) paramValue;
            }
        }
        return defaultParam;
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
        return true;
    }


    public Map<String, Object> getProperties() {
        return properties;
    }


    public void setProperties( Map<String, Object> properties ) {
        this.properties = properties;

        // add default values to properties map
        addDefaultsToProperties();
    }

    public void addProperties( Map<String, Object> properties ) {
        this.properties.putAll(properties);

        // add default values to properties map
        addDefaultsToProperties();
    }
}
