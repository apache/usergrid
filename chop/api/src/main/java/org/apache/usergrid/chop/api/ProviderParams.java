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
package org.apache.usergrid.chop.api;


import java.util.Map;

/**
 * This contains the parameters necessary to manage all environment dependant cluster operations,
 * such as creating, launching, destroying instances
 */
public interface ProviderParams {

    /**
     * @return  User owning these parameters
     */
    String getUsername();


    /**
     * @return  Instance type of virtual or container based instances
     *          to be used on setup, corresponds to Instance Type on AWS
     */
    String getInstanceType();


    /**
     * @return  Access Key to be used while communicating with Provider
     */
    String getAccessKey();


    /**
     * @return  Secret Key to be used while communicating with Provider
     */
    String getSecretKey();


    /**
     * @return  Base image id to be used when setting up runner instances,
     *          corresponds to AMI ID for AWS
     */
    String getImageId();


    /**
     * Path to key files identified by key-pair-names.
     */
    Map<String, String> getKeys();


    /**
     * @return  Key name for use on SSH operations to runner instances,
     *          corresponds to Key pair name on AWS
     */
    String getKeyName();
}
