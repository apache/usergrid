/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp.dao.model;


import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.usergrid.chop.api.ProviderParams;

import java.util.HashMap;
import java.util.Map;


/**
 * Holds the provider (AWS for instance) specific information to be used
 * when setting up runner instances.
 * <p>
 * Note that each username has one and only one ProviderParams object stored on elastic search,
 * so the username is used as ID in its persistence operations.
 */
public class BasicProviderParams implements ProviderParams {

    private String username;
    private String instanceType;
    private String accessKey;
    private String secretKey;
    private String imageId;
    private String keyName;
    private Map<String, String> keys = new HashMap<String, String>();


    /**
     * This is for new users, who haven't provided their parameters yet.
     *
     * @param username    User who owns these parameters
     */
    public BasicProviderParams( String username ) {
        this( username, "", "", "", "", "" );
    }


    /**
     * @param username          User who owns these parameters
     * @param instanceType  Which type of virtual or container based instances will be used on setup
     * @param accessKey     Access Key to be used while communicating with Provider
     * @param secretKey     Secret Key to be used while communicating with Provider
     * @param imageId       Base image id of runner instances to be setup, corresponds to AMI ID for AWS
     * @param keyName       Key name for use on SSH operations to runner instances,
     *                      corresponds to Key pair name on AWS
     */
    public BasicProviderParams( String username, String instanceType, String accessKey, String secretKey,
                                String imageId, String keyName ) {

        this.username = username;
        this.instanceType = instanceType;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.imageId = imageId;
        this.keyName = keyName;
    }


    /**
     * @return  User owning these parameters
     */
    @Override
    public String getUsername() {
        return username;
    }


    /**
     * @return  Instance type of virtual or container based instances
     *          to be used on setup, corresponds to Instance Type on AWS
     */
    @Override
    public String getInstanceType() {
        return instanceType;
    }


    /**
     * @return  Access Key to be used while communicating with Provider
     */
    @Override
    public String getAccessKey() {
        return accessKey;
    }


    /**
     * @return  Secret Key to be used while communicating with Provider
     */
    @Override
    public String getSecretKey() {
        return secretKey;
    }


    /**
     * @return  Base image id to be used when setting up runner instances,
     *          corresponds to AMI ID for AWS
     */
    @Override
    public String getImageId() {
        return imageId;
    }


    /**
     * @return  Key name for use on SSH operations to runner instances,
     *          corresponds to Key pair name on AWS
     */
    @Override
    public String getKeyName() {
        return keyName;
    }


    /**
     * @param keyName   Key name for use on SSH operations to runner instances,
     *                  corresponds to Key pair name on AWS
     */
    public void setKeyName( String keyName ) {
        this.keyName = keyName;
    }


    /**
     * Gets the stored map of keys for this username.
     * <p>
     * <ul>
     *     <li>Key of the map corresponds to the key name on provider (Key pair name for AWS)</li>
     *     <li>Value corresponds to the full file path of the actual key file on file system</li>
     * </ul>
     *
     * @return  Stored map of keys for <code>username</code>
     */
    @Override
    public Map<String, String> getKeys() {
        return keys;
    }


    /**
     * Sets the stored map of keys for this username.
     * <p>
     * <ul>
     *     <li>Key of the map should correspond to the key name on provider (Key pair name for AWS)</li>
     *     <li>Value should correspond to the full file path of the actual key file on file system</li>
     * </ul>
     *
     * @param keys  Stored map of keys for <code>username</code>
     */
    public void setKeys( Map<String, String> keys ) {
        this.keys = keys;
    }


    /**
     * @return  all parameters this object contains
     */
    @Override
    public String toString() {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
                .append( "username", username )
                .append( "instanceType", instanceType )
                .append( "accessKey", accessKey )
                .append( "secretKey", secretKey )
                .append( "imageId", imageId )
                .append( "keyName", keyName )
                .append( "keys", keys )
                .toString();
    }
}
