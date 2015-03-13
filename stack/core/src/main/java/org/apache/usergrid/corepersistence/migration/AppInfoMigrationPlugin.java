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
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
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

import static org.apache.usergrid.persistence.Schema.PROPERTY_APPLICATION_ID;
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

    @Inject
    protected MigrationInfoSerialization migrationInfoSerialization;

    @Inject
    protected EntityManagerFactory emf; // protected for test purposes only

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public void run(ProgressObserver observer) {

        final int version = migrationInfoSerialization.getVersion( getName() );

        if ( version == getMaxVersion() ) {
            logger.debug("Skipping Migration Plugin: " + getName());
            return;
        }

        observer.start();

        // Search the old and now deprecated System App for appinfo entities

        EntityManager systemAppEm = emf.getEntityManager( CpNamingUtils.SYSTEM_APP_ID );
        Query q = Query.fromQL("select *");
        Results results;
        try {
            results = systemAppEm.searchCollection(systemAppEm.getApplicationRef(), "appinfos", q);
        } catch (Exception e) {
            logger.error("Error reading old appinfos collection, not migrating", e);
            return;
        }

        if ( !results.isEmpty() ) {

            // we found appinfos, let's migrate them to application_infos in the Management App

            EntityManager em = emf.getEntityManager( emf.getManagementAppId());
            String currentAppName = null;
            try {
                logger.info("Migrating old appinfos");

                for (Entity oldAppInfo : results.getEntities()) {

                    final String appName = currentAppName = oldAppInfo.getName();

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

                    Entity appInfo = getApplicationInfo( emf, appId );
                    if ( appInfo == null ) {
                        Map<String, Object> appInfoMap = new HashMap<String, Object>() {{
                            put(PROPERTY_NAME, appName);
                            put(PROPERTY_APPLICATION_ID, appId);
                        }};
                        appInfo = em.create(appId, CpNamingUtils.APPLICATION_INFO, appInfoMap);
                        observer.update( getMaxVersion(), "Created application_info for " + appName);

                    } else {
                        appInfo.setProperty(PROPERTY_APPLICATION_ID, appId);
                        em.update(appInfo);
                        observer.update( getMaxVersion(), "Updated existing application_info for " + appName);
                    }
                    em.createConnection(new SimpleEntityRef(Group.ENTITY_TYPE, organizationId), "owns", appInfo);
                }

                em.refreshIndex();

                // after we've successfully created all of the application_infos, we delete the old appoinfos

                for (Entity oldAppInfo : results.getEntities()) {
                    em.delete(oldAppInfo);
                }

                migrationInfoSerialization.setVersion( getName(), getMaxVersion() );

            } catch (Exception e) {

                // stop on any exception and return failure

                String msg = "Exception writing application_info for " + currentAppName;
                logger.error(msg, e);
                observer.failed( getMaxVersion(), msg);
            }

        } else {
            logger.info("No old appinfos found, no need for migration");
        }

        observer.complete();

    }

    private Entity getApplicationInfo( EntityManagerFactory emf, UUID appId ) throws Exception {

        UUID mgmtAppId = emf.getManagementAppId();
        EntityManager rootEm = emf.getEntityManager( mgmtAppId );

        final Results applicationInfoResults = rootEm.searchCollection(
            new SimpleEntityRef("application", mgmtAppId), "application_infos",
            Query.fromQL("select * where applicationId=" + appId.toString()));

        return applicationInfoResults.getEntity();
    }

    @Override
    public int getMaxVersion() {
        return 1; // standalone plugin, happens once
    }

    @Override
    public PluginPhase getPhase() {
        return PluginPhase.MIGRATE;
    }
}
