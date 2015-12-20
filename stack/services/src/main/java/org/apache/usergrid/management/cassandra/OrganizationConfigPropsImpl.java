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
package org.apache.usergrid.management.cassandra;


import org.apache.usergrid.management.AccountCreationProps;
import org.apache.usergrid.management.OrganizationConfigProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.Properties;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.usergrid.utils.ListUtils.anyNull;


public class OrganizationConfigPropsImpl implements OrganizationConfigProps {
    private static final Logger logger = LoggerFactory.getLogger( OrganizationConfigPropsImpl.class );

    protected final Properties properties;

    public OrganizationConfigPropsImpl(Properties properties) {
        this.properties = properties;
    }


    public String getProperty( String name ) {
        String propertyValue = properties.getProperty( name );
        if ( isBlank( propertyValue ) ) {
            logger.warn( "Missing value for " + name );
            propertyValue = null;
        }
        return propertyValue;
    }


    public String getProperty( String name, String defaultValue ) {
        return properties.getProperty( name, defaultValue );
    }


    public boolean isProperty( String name ) {
        return parseBoolean( getProperty( name ) );
    }

    public int intProperty( String name, String defaultValue ) {
        return parseInt( getProperty( name, defaultValue ) );
    }

    public void setProperty( String name, String value ) {
        properties.setProperty( name, value );
    }

}
