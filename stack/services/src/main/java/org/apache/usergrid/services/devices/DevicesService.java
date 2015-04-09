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
package org.apache.usergrid.services.devices;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.Device;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class DevicesService extends AbstractCollectionService {

    private static final Logger logger = LoggerFactory.getLogger( DevicesService.class );


    public DevicesService() {
        super();
        logger.debug( "/devices" );
    }


    @Override
    public ServiceResults putItemById( ServiceContext context, UUID id ) throws Exception {
        logger.debug("Registering device {}", id);
        return super.putItemById( context, id );
    }


    @Override
    public ServiceResults postItemById( ServiceContext context, UUID id ) throws Exception {
        logger.info("Attempting to connect an entity to device {}", id);
        return super.postItemById( context, id );
    }

    protected void deleteEntityConnection(final EntityRef deviceRef, final EntityRef owner){
        if(deviceRef == null) {
            return;
        }
        try {
            Results entities = em.getCollection(deviceRef,"users",null,100, Query.Level.REFS,false);
            Observable.from(entities.getEntities())
                    .map(new Func1<Entity, Boolean>() {
                        @Override
                        public Boolean call(Entity user) {
                            boolean removed = false;
                            try {
                                if(!user.getUuid().equals(owner.getUuid())) { //skip current user
                                    Results devicesResults = em.getCollection(user, "devices", null, 100, Query.Level.REFS, false);
                                    List<Entity> userDevices = devicesResults.getEntities();
                                    for (EntityRef userDevice : userDevices) {
                                        if(userDevice.getUuid().equals(deviceRef.getUuid())) { //only remove the current device from user
                                            em.removeFromCollection(user, "devices", userDevice);
                                        }
                                    }
                                    em.removeFromCollection(deviceRef, "users", user);
                                    removed = true;
                                }
                            } catch (Exception e) {
                                logger.error("Failed to delete connection " + user.toString(), e);
                            }
                            return removed;
                        }
                    }).toBlocking().lastOrDefault(null);
        }catch (Exception e){
            logger.error("failed to get connection",e);
        }
    }
}
