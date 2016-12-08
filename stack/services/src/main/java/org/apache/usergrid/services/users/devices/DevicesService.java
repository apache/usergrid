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
package org.apache.usergrid.services.users.devices;


import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServiceResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


public class DevicesService extends org.apache.usergrid.services.devices.DevicesService {

    private static final Logger logger = LoggerFactory.getLogger( DevicesService.class );


    public DevicesService() {
        super();
        if (logger.isTraceEnabled()) {
            logger.trace("/users/*/devices");
        }
    }

    @Override
    public ServiceResults putItemById( ServiceContext context, UUID id ) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("Registering device {}", id);
        }
        unregisterDeviceToUsers(id,context.getOwner());
        return super.putItemById( context, id );
    }


    @Override
    public ServiceResults postItemById( ServiceContext context, UUID id ) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("Attempting to connect an entity to device {}", id);
        }
        unregisterDeviceToUsers(id,context.getOwner());
        return super.postItemById( context, id );
    }

    protected void unregisterDeviceToUsers(UUID deviceId, EntityRef owner){
        try {
            EntityRef device = new SimpleEntityRef("device",deviceId);
            deleteEntityConnection(device,owner);
        } catch (Exception e) {
            logger.error("Failed to delete connection for {}", deviceId.toString(), e);
        }

    }
}
