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
package org.apache.usergrid.services;


import java.util.UUID;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;


public interface Service {

    public static final String GENERIC_ENTITY_TYPE = "entity";

    public String getServiceType();

    public Class<? extends Entity> getEntityClass();

    public String getEntityType();

    public boolean isRootService();

    public ServiceResults invoke( ServiceAction action, ServiceRequest request, ServiceResults previousResults,
                                  ServicePayload payload ) throws Exception;

    public Entity getEntity( ServiceRequest request, UUID uuid ) throws Exception;

    public Entity getEntity( ServiceRequest request, String name ) throws Exception;

    public Entity importEntity( ServiceRequest request, Entity entity ) throws Exception;

    public Entity writeEntity( ServiceRequest request, Entity entity ) throws Exception;

    public Entity updateEntity( ServiceRequest request, EntityRef ref, ServicePayload payload ) throws Exception;
}
