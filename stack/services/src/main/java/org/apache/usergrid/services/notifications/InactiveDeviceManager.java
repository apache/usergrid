/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.services.notifications;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.services.notifications.impl.ApplicationQueueManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * remove inactive devices.
 */
public class InactiveDeviceManager {
    private static final Logger LOG = LoggerFactory.getLogger(InactiveDeviceManager.class);
    private final Notifier notifier;
    private EntityManager entityManager;

    public InactiveDeviceManager(Notifier notifier,EntityManager entityManager){
        this.notifier = notifier;
        this.entityManager = entityManager;
    }
    public void removeInactiveDevices( Map<String,Date> inactiveDeviceMap  ){
        final String notfierPostFix = ApplicationQueueManagerImpl.NOTIFIER_ID_POSTFIX;
        if (inactiveDeviceMap != null && inactiveDeviceMap.size() > 0) {
            LOG.debug("processing {} inactive devices",  inactiveDeviceMap.size());
            Map<String, Object> clearPushtokenMap = new HashMap<String, Object>( 2);
            clearPushtokenMap.put(notifier.getName() + notfierPostFix,  "");
            clearPushtokenMap.put(notifier.getUuid() + notfierPostFix,  "");

            // todo: this could be done in a single query
            for (Map.Entry<String, Date> entry : inactiveDeviceMap.entrySet()) {
                try {
                    // name
                    Query query = Query.fromQL( notifier.getName() + notfierPostFix  + " = '" + entry.getKey() + "'");
                    Results results = entityManager.searchCollection(entityManager.getApplication(), "devices", query);
                    for (Entity e : results.getEntities()) {
                        entityManager.updateProperties(e, clearPushtokenMap);
                    }
                    // uuid
                    query = Query.fromQL( notifier.getName() + notfierPostFix  + " = " + entry.getKey() + "");
                    results = entityManager.searchCollection(entityManager.getApplication(),  "devices", query);
                    for (Entity e : results.getEntities()) {
                        entityManager.updateProperties(e, clearPushtokenMap);
                    }
                }catch (Exception e){
                    LOG.error("failed to remove token",e);
                }
            }
        }
    }
}
