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
package org.apache.usergrid.corepersistence.migration;

import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.core.migration.data.MigrationPlugin;
import org.apache.usergrid.persistence.core.migration.data.PluginPhase;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;


/**
 * Migration of appinfos collection to application_info collection.
 *
 * Part of USERGRID-448 "Remove redundant appinfos collections in ManagementServiceImpl"
 * https://issues.apache.org/jira/browse/USERGRID-448
 */
public class AppInfoMigrationPlugin implements MigrationPlugin {
    private static final Logger logger = LoggerFactory.getLogger(AppInfoMigrationPlugin.class);

    public static String PLUGIN_NAME = "appinfo-migration";

    // protected for test purposes only
    @Inject
    protected EntityManagerFactory emf;

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public void run(ProgressObserver observer) {

        observer.start();

        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID);

        Query q = Query.fromQL("select *");
        Results results;
        try {
            results = em.searchCollection(em.getApplicationRef(), "appinfos", q);
        } catch (Exception e) {
            logger.error("Error reading old appinfos collection, not migrating", e);
            return;
        }

        if ( !results.isEmpty() ) {

            // applications still found in old appinfos collection, migrate them.
            logger.info("Migrating old appinfos");

            try {

                for (Entity oldAppInfo : results.getEntities()) {

                    final String appName = oldAppInfo.getName();

                    UUID applicationId;
                    UUID organizationId;

                    Object uuidObject = oldAppInfo.getProperty("applicationUuid");
                    if (uuidObject instanceof UUID) {
                        applicationId = (UUID) uuidObject;
                    } else {
                        applicationId = UUIDUtils.tryExtractUUID(uuidObject.toString());
                    }
                    uuidObject = oldAppInfo.getProperty("organizationUuid");
                    if (uuidObject instanceof UUID) {
                        organizationId = (UUID) uuidObject;
                    } else {
                        organizationId = UUIDUtils.tryExtractUUID(uuidObject.toString());
                    }

                    // create and connect new APPLICATION_INFO oldAppInfo to Organization

                    final UUID appId = applicationId;
                    Map<String, Object> appInfoMap = new HashMap<String, Object>() {{
                        put(PROPERTY_NAME, appName);
                        put(PROPERTY_UUID, appId);
                    }};

                    final Entity appInfo;
                    appInfo = em.create(appId, CpNamingUtils.APPLICATION_INFO, appInfoMap);
                    em.createConnection(new SimpleEntityRef(Group.ENTITY_TYPE, organizationId), "owns", appInfo);
                    em.delete(oldAppInfo);

                    observer.update( getMaxVersion(), "Updated application " + appName);
                }

                em.refreshIndex();

            } catch (Exception e) {
                String msg = "Exception writing new application_info collection";
                logger.error(msg, e);
                observer.failed( getMaxVersion(), msg);
            }

        } else {
            logger.info("No old appinfos found, no need for migration");
        }

        observer.complete();

    }

    @Override
    public int getMaxVersion() {
        return CoreDataVersions.APPINFO_FIX.getVersion();
    }

    @Override
    public PluginPhase getPhase() {
        return PluginPhase.MIGRATE;
    }
}
