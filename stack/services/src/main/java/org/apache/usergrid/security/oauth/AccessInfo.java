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
package org.apache.usergrid.security.oauth;


import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;



@XmlRootElement
public class AccessInfo {

    String accessToken;
    String tokenType;
    long expiresIn;
    String refreshToken;
    String scope;
    String state;
    Long passwordChanged;

    protected Map<String, Object> properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );


    public AccessInfo() {

    }


    @JsonProperty("access_token")
    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getAccessToken() {
        return accessToken;
    }


    @JsonProperty("access_token")
    public void setAccessToken( String accessToken ) {
        this.accessToken = accessToken;
    }


    public AccessInfo withAccessToken( String accessToken ) {
        this.accessToken = accessToken;
        return this;
    }


    @JsonProperty("token_type")
    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getTokenType() {
        return tokenType;
    }


    @JsonProperty("token_type")
    public void setTokenType( String tokenType ) {
        this.tokenType = tokenType;
    }


    public AccessInfo withTokenType( String tokenType ) {
        this.tokenType = tokenType;
        return this;
    }


    @JsonProperty("expires_in")
    public long getExpiresIn() {
        return expiresIn;
    }


    @JsonProperty("expires_in")
    public void setExpiresIn( long expiresIn ) {
        this.expiresIn = expiresIn;
    }


    public AccessInfo withExpiresIn( long expiresIn ) {
        this.expiresIn = expiresIn;
        return this;
    }


    @JsonProperty("refresh_token")
    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getRefreshToken() {
        return refreshToken;
    }


    @JsonProperty("refresh_token")
    public void setRefreshToken( String refreshToken ) {
        this.refreshToken = refreshToken;
    }


    public AccessInfo withRefreshToken( String refreshToken ) {
        this.refreshToken = refreshToken;
        return this;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getScope() {
        return scope;
    }


    public void setScope( String scope ) {
        this.scope = scope;
    }


    public AccessInfo withScope( String scope ) {
        this.scope = scope;
        return this;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getState() {
        return state;
    }


    public void setState( String state ) {
        this.state = state;
    }


    public AccessInfo withState( String state ) {
        this.state = state;
        return this;
    }


    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }


    @JsonAnySetter
    public void setProperty( String key, Object value ) {
        properties.put( key, value );
    }


    public AccessInfo withProperty( String key, Object value ) {
        properties.put( key, value );
        return this;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Long getPasswordChanged() {
        return passwordChanged;
    }


    public AccessInfo withPasswordChanged( Long lastPasswordChange ) {
        this.passwordChanged = lastPasswordChange;
        return this;
    }
}
