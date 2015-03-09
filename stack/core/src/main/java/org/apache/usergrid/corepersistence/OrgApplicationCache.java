/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence;


import java.util.UUID;

import org.apache.usergrid.persistence.Entity;

import com.google.common.base.Optional;


/**
 * A simple cache interface for looking up entities from an EM
 */
public interface OrgApplicationCache {


    /**
     * Get an entity by it's alias property.  The result is cached. To clear it call evict or evict all
     * @param
     * @return
     */
    public Optional<UUID> getOrganizationId(final String orgName);

    /**
     * Evict the org by name
     * @param orgName
     */
    public void evictOrgId(final String orgName);

    /**
     * Evict the application by name
     * @param applicationName
     * @return
     */
    public Optional<UUID> getApplicationId(final String applicationName);


    /**
     * Evict the app id by the name
     */
    public void evictAppId(final String applicationname);


    /**
     * Evict all caches
     */
    public void evictAll();
}
