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
package org.apache.usergrid.security.tokens;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.security.AuthPrincipalInfo;


public class TokenInfo {

    UUID uuid;
    String type;
    long created;
    long accessed;
    long inactive;
    // total duration, in milliseconds
    long duration;
    AuthPrincipalInfo principal;
    Map<String, Object> state;
    UUID workflowOrgId;


    public TokenInfo( UUID uuid, String type, long created, long accessed, long inactive, long duration,
                      AuthPrincipalInfo principal, Map<String, Object> state ) {
        this(uuid, type, created, accessed, inactive, duration, principal, state, null);
    }


    public TokenInfo( UUID uuid, String type, long created, long accessed, long inactive, long duration,
                      AuthPrincipalInfo principal, Map<String, Object> state, UUID workflowOrgId ) {
        this.uuid = uuid;
        this.type = type;
        this.created = created;
        this.accessed = accessed;
        this.inactive = inactive;
        this.principal = principal;
        this.duration = duration;
        this.state = state;
        this.workflowOrgId = workflowOrgId;
    }


    public UUID getUuid() {
        return uuid;
    }


    public void setUuid( UUID uuid ) {
        this.uuid = uuid;
    }


    public String getType() {
        return type;
    }


    public void setType( String type ) {
        this.type = type;
    }


    public long getCreated() {
        return created;
    }


    public void setCreated( long created ) {
        this.created = created;
    }


    /** @return the expiration */
    public long getDuration() {
        return duration;
    }


    /** If the expiration is undefined, return the default that's been passed in */
    public long getExpiration( long defaultExpiration ) {
        if ( duration == 0 ) {
            return defaultExpiration;
        }
        return duration;
    }


    /** @param expiration the expiration to set */
    public void setDuration( long expiration ) {
        this.duration = expiration;
    }


    public long getAccessed() {
        return accessed;
    }


    public void setAccessed( long accessed ) {
        this.accessed = accessed;
    }


    public long getInactive() {
        return inactive;
    }


    public void setInactive( long inactive ) {
        this.inactive = inactive;
    }


    public AuthPrincipalInfo getPrincipal() {
        return principal;
    }


    public void setPrincipal( AuthPrincipalInfo principal ) {
        this.principal = principal;
    }


    public Map<String, Object> getState() {
        return state;
    }


    public void setState( Map<String, Object> state ) {
        this.state = state;
    }


    public UUID getWorkflowOrgId() {
        return workflowOrgId;
    }


    public void setWorkflowOrgId( UUID workflowOrgId ) {
        this.workflowOrgId = workflowOrgId;
    }
}
