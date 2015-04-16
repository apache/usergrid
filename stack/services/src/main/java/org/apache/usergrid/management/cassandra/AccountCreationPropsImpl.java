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


import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.AccountCreationProps;

import static java.lang.Boolean.parseBoolean;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.usergrid.utils.ListUtils.anyNull;


public class AccountCreationPropsImpl implements AccountCreationProps {
    private static final Logger logger = LoggerFactory.getLogger( AccountCreationPropsImpl.class );

    protected final Properties properties;
    private final SuperUser superUser;


    public AccountCreationPropsImpl( Properties properties ) {
        this.properties = properties;
        this.superUser = new SuperUserImpl(properties);
    }


    public boolean newOrganizationsNeedSysAdminApproval() {
        return isProperty( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS );
    }


    public boolean newAdminUsersNeedSysAdminApproval() {
        return isProperty( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS );
    }


    public boolean newAdminUsersRequireConfirmation() {
        return isProperty( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION );
    }


    public boolean newOrganizationsRequireConfirmation() {
        return isProperty( PROPERTIES_ORGANIZATIONS_REQUIRE_CONFIRMATION );
    }


    public boolean notifySysAdminOfNewAdminUsers() {
        return isProperty( PROPERTIES_NOTIFY_SYSADMIN_OF_NEW_ADMIN_USERS );
    }


    public boolean notifySysAdminOfNewOrganizations() {
        return isProperty( PROPERTIES_NOTIFY_SYSADMIN_OF_NEW_ORGANIZATIONS );
    }


    public boolean notifyAdminOfActivation() {
        return isProperty( PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION );
    }

    public int getMaxOrganizationsForSuperUserLogin() {
        return intProperty( PROPERTIES_USERGRID_SYSADMIN_LOGIN_FETCH_ORGS, "10" );
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


    public Properties getMailProperties() {
        Properties p = new Properties();
        for ( Enumeration e = properties.propertyNames(); e.hasMoreElements(); ) {
            String name = ( String ) e.nextElement();
            if ( name.startsWith( "mail." ) ) {
                p.setProperty( name, properties.getProperty( name ) );
            }
        }
        return p;
    }

    public SuperUser getSuperUser() {
        return superUser;
    }

    protected static class SuperUserImpl implements SuperUser {
        private final boolean enabled;
        private final String username;
        private final String email;
        private final String password;

        public SuperUserImpl(Properties properties) {
            enabled = parseBoolean(properties.getProperty(PROPERTIES_SYSADMIN_LOGIN_ALLOWED));
            username = properties.getProperty(PROPERTIES_SYSADMIN_LOGIN_NAME);
            email = properties.getProperty(PROPERTIES_SYSADMIN_LOGIN_EMAIL);
            password = properties.getProperty(PROPERTIES_SYSADMIN_LOGIN_PASSWORD);
        }

        @Override
        public boolean isEnabled() {
            return superuserEnabled();
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public String getPassword() {
            return password;
        }

        private boolean superuserEnabled() {
            return enabled && !anyNull(username, email, password);
        }
    }
}
