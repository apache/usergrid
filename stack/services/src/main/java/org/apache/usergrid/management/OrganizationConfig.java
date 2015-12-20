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


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class OrganizationConfig {

    private static final String DEFAULT_CONNECTION_PARAM_DEFAULTVALUE = "all";

    private static final String ADMIN_SYSADMIN_EMAIL_DEFAULTVALUE = null; // null will fall back to system level admin

    private static final String ADMIN_ACTIVATION_URL_DEFAULTVALUE = ""; // should be configured in properties file

    private static final String ADMIN_CONFIRMATION_URL_DEFAULTVALUE = ""; // should be configured in properties file

    private static final String ADMIN_RESETPW_URL_DEFAULTVALUE = ""; // should be configured in properties file

    // properties in property file and
    private static final String [] propertyNames = {
            OrganizationConfigProps.PROPERTIES_DEFAULT_CONNECTION_PARAM,
            OrganizationConfigProps.PROPERTIES_ADMIN_SYSADMIN_EMAIL,
            OrganizationConfigProps.PROPERTIES_ADMIN_ACTIVATION_URL,
            OrganizationConfigProps.PROPERTIES_ADMIN_CONFIRMATION_URL,
            OrganizationConfigProps.PROPERTIES_ADMIN_RESETPW_URL
    };

    // values to use if not found in config files
    private static final String [] noConfigDefaults = {
            DEFAULT_CONNECTION_PARAM_DEFAULTVALUE,
            ADMIN_SYSADMIN_EMAIL_DEFAULTVALUE,
            ADMIN_ACTIVATION_URL_DEFAULTVALUE,
            ADMIN_CONFIRMATION_URL_DEFAULTVALUE,
            ADMIN_RESETPW_URL_DEFAULTVALUE
    };

    private Map<String, String> defaultProperties;

    private UUID id;
    private String name;
    private Map<String, Object> orgConfigProperties;

    private void setDefaultProperties(OrganizationConfigProps configFileProperties) {
        defaultProperties = new HashMap<>();
        for (int i = 0; i < propertyNames.length; i++) {
            String propertyValue = configFileProperties != null ? configFileProperties.getProperty(propertyNames[i]) : null;
            defaultProperties.put(propertyNames[i], propertyValue != null ? propertyValue : noConfigDefaults[i]);
        }
    }

    private OrganizationConfig() {
    }

    public OrganizationConfig(OrganizationConfigProps configFileProperties) {
        setDefaultProperties(configFileProperties);
        // will add id and name once default org exists
    }

    public OrganizationConfig(OrganizationConfigProps configFileProperties, UUID id, String name) {
        setDefaultProperties(configFileProperties);
        this.id = id;
        this.name = name;
    }

    public OrganizationConfig(OrganizationConfigProps configFileProperties, UUID id, String name, Map<String, Object> entityProperties) {

        this( configFileProperties, id, name );
        setOrgConfigProperties(entityProperties);
    }

    // replaces properties with supplied properties, and then adds defaults
    private void setOrgConfigProperties(Map<String, Object> orgConfigProperties) {
        this.orgConfigProperties = orgConfigProperties;

        // add default values to properties map
        addDefaultstoConfigProperties();
    }

    // adds supplied properties to existing properties, and then adds defaults
    public void addProperties( Map<String, Object> orgConfigProperties ) {
        this.orgConfigProperties.putAll(orgConfigProperties);

        // add default values to properties map
        addDefaultstoConfigProperties();
    }

    private void addDefaultstoConfigProperties()  {
        for (int i=0; i < propertyNames.length; i++) {
            if (!orgConfigProperties.containsKey(propertyNames[i])) {
                orgConfigProperties.put(propertyNames[i], defaultProperties.get(propertyNames[i]));
            }
        }
    }


    public Map<String, Object> getOrgConfigProperties() {
        return orgConfigProperties;
    }

    public String getProperty(String key) {
        String retValue = null;
        if (orgConfigProperties != null) {
            Object value = orgConfigProperties.get(key);
            if (value instanceof String) {
                retValue = (String) value;
            }
        }
        return retValue != null ? retValue : defaultProperties.get(key);
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
        String defaultParam = DEFAULT_CONNECTION_PARAM_DEFAULTVALUE;
        if ( orgConfigProperties != null ) {
            Object paramValue = orgConfigProperties.get( OrganizationConfigProps.PROPERTIES_DEFAULT_CONNECTION_PARAM );
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

}
