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

import javax.xml.bind.annotation.XmlRootElement;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.persistence.Schema.PROPERTY_ACTIVATED;
import static org.apache.usergrid.persistence.Schema.PROPERTY_ADMIN;
import static org.apache.usergrid.persistence.Schema.PROPERTY_CONFIRMED;
import static org.apache.usergrid.persistence.Schema.PROPERTY_DISABLED;
import static org.apache.usergrid.persistence.Schema.PROPERTY_EMAIL;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_USERNAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.utils.ConversionUtils.getBoolean;
import static org.apache.usergrid.utils.ConversionUtils.string;
import static org.apache.usergrid.utils.ConversionUtils.uuid;


@XmlRootElement
public class UserInfo {

    private UUID applicationId;
    private UUID id;
    private String username;
    private String name;
    private String email;
    private boolean activated;
    private boolean confirmed;
    private boolean disabled;
    private Map<String, Object> properties;
    private boolean admin;

    public UserInfo() {}

    public UserInfo( UUID applicationId, UUID id, String username, String name, String email, boolean confirmed,
                     boolean activated, boolean disabled, Map<String, Object> properties, boolean admin ) {
        this.applicationId = applicationId;
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.confirmed = confirmed;
        this.activated = activated;
        this.disabled = disabled;
        this.properties = properties;
        this.admin = admin;
    }


    public UserInfo( UUID applicationId, Map<String, Object> properties ) {
        this.applicationId = applicationId;
        id = uuid( properties.remove( PROPERTY_UUID ) );
        username = string( properties.remove( PROPERTY_USERNAME ) );
        name = string( properties.remove( PROPERTY_NAME ) );
        email = string( properties.remove( PROPERTY_EMAIL ) );
        confirmed = getBoolean( properties.remove( PROPERTY_CONFIRMED ) );
        activated = getBoolean( properties.remove( PROPERTY_ACTIVATED ) );
        disabled = getBoolean( properties.remove( PROPERTY_DISABLED ) );
        admin = getBoolean( properties.remove( PROPERTY_ADMIN) );
        this.properties = properties;
    }


    public UUID getApplicationId() {
        return applicationId;
    }


    public UUID getUuid() {
        return id;
    }


    public String getName() {
        return name;
    }


    public String getUsername() {
        return username;
    }


    public String getEmail() {
        return email;
    }


    @Override
    public String toString() {
        return id + "/" + name + "/" + email;
    }


    public String getDisplayEmailAddress() {
        if ( isNotBlank( name ) ) {
            return name + " <" + email + ">";
        }
        return email;
    }


    public String getHTMLDisplayEmailAddress() {
        if ( isNotBlank( name ) ) {
            return name + " &lt;<a href=\"mailto:" + email + "\">" + email + "</a>&gt;";
        }
        return email;
    }


    public boolean isActivated() {
        return activated;
    }


    public boolean isDisabled() {
        return disabled;
    }


    public boolean isAdminUser() {
        return admin;
    }


    public Map<String, Object> getProperties() {
        return properties;
    }


    public boolean isConfirmed() {
        return confirmed;
    }


}
