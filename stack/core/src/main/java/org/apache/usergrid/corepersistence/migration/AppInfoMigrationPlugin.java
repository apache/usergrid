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

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.migration.data.MigrationPlugin;
import org.apache.usergrid.persistence.core.migration.data.PluginPhase;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.*;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getApplicationScope;
import static org.apache.usergrid.persistence.Schema.*;


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
    final private MigrationInfoSerialization migrationInfoSerialization;

    @Inject
    final private EntityManagerFactory emf;

    @Inject
    final private EntityCollectionManagerFactory entityCollectionManagerFactory;

    @Inject
    final private GraphManagerFactory graphManagerFactory;


    @Inject
    public AppInfoMigrationPlugin(
        EntityManagerFactory emf,
        MigrationInfoSerialization migrationInfoSerialization,
        EntityCollectionManagerFactory entityCollectionManagerFactory,
        GraphManagerFactory graphManagerFactory ) {

        this.emf = emf;
        this.migrationInfoSerialization = migrationInfoSerialization;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.graphManagerFactory = graphManagerFactory;
    }


    @Override
    public String getName() {
        return PLUGIN_NAME;
    }


    @Override
    public int getMaxVersion() {
        return 2; // standalone plugin, happens once
    }


    @Override
    public PluginPhase getPhase() {
        return PluginPhase.MIGRATE;
    }


    @Override
    public void run(ProgressObserver observer) {

        final int version = migrationInfoSerialization.getVersion(getName());

        if (version == getMaxVersion()) {
            logger.debug("Skipping Migration Plugin: " + getName());
            return;
        }
        observer.start();
        //get old app infos to migrate
        final Observable<org.apache.usergrid.persistence.model.entity.Entity> oldAppInfos = getOldAppInfos();
        oldAppInfos
            .doOnNext(oldAppInfoEntity -> {
                try {
                    migrateAppInfo( oldAppInfoEntity, observer);
                }catch (Exception e){
                    logger.error("Failed to migrate app info"+oldAppInfoEntity.getId().getUuid(),e);
                    throw new RuntimeException(e);
                }

            })
            .doOnCompleted(() -> {
                migrationInfoSerialization.setVersion(getName(), getMaxVersion());
                observer.complete();
            }).toBlocking().lastOrDefault(null);

    }


    private void migrateAppInfo( org.apache.usergrid.persistence.model.entity.Entity oldAppInfoEntity, ProgressObserver observer) {
        // Get appinfos from the Graph, we don't expect many so use iterator
        final EntityManager managementEm = emf.getManagementEntityManager();

        Map oldAppInfoMap = CpEntityMapUtils.toMap(oldAppInfoEntity);

        final String name = (String) oldAppInfoMap.get(PROPERTY_NAME);

        try {
            final String orgName = name.split("/")[0];
            final String appName = name.split("/")[1];
            UUID applicationId = getUuid(oldAppInfoMap,"applicationUuid");

            //get app info from graph to see if it has been migrated already
            Entity appInfo = getApplicationInfo(applicationId);
            if (appInfo == null) {
                // create and connect new APPLICATION_INFO oldAppInfo to Organization
                appInfo = createNewAppInfo(managementEm, name, applicationId);
                observer.update(getMaxVersion(), "Created application_info for " + appName);
                // create org->app connections, but not for apps in dummy "usergrid" internal organization
                if (!orgName.equals("usergrid")) {
                    EntityRef orgRef = managementEm.getAlias(Group.ENTITY_TYPE, orgName);
                    managementEm.createConnection(orgRef, "owns", appInfo);
                }
            } else {
                //already migrated don't do anything
                observer.update(getMaxVersion(), "Received existing application_info for " + appName + " don't do anything");
            }

        } catch (Exception e) {
            String msg = "Exception writing application_info for " + name;
            logger.error(msg, e);
            observer.failed(getMaxVersion(), msg);
            throw new RuntimeException(e);
        }

    }

    private UUID getUuid(Map oldAppInfoMap, String key) {
        UUID applicationId;
        Object uuidObject = oldAppInfoMap.get(key);
        if (uuidObject instanceof UUID) {
            applicationId = (UUID) uuidObject;
        } else {
            applicationId = UUIDUtils.tryExtractUUID(uuidObject.toString());
        }
        return applicationId;
    }

    private Entity createNewAppInfo(EntityManager managementEm, final String name, final UUID applicationId) throws Exception {
        Entity appInfo;Map<String, Object> appInfoMap = new HashMap<String, Object>() {{
            put(PROPERTY_NAME, name);
            put(PROPERTY_APPLICATION_ID, applicationId);
        }};
        appInfo = managementEm.create(new SimpleId(applicationId, CpNamingUtils.APPLICATION_INFO), appInfoMap);
        return appInfo;
    }

    private void deleteOldAppInfo(UUID uuid) {
        final ApplicationScope systemAppScope = getApplicationScope(CpNamingUtils.SYSTEM_APP_ID );
        final EntityCollectionManager systemCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( systemAppScope );
        systemCollectionManager.mark( new SimpleId( uuid, "appinfos" ) ).toBlocking().last();
    }


    /**
     * TODO: Use Graph to get application_info for an specified Application.
     */
    private Entity getApplicationInfo( final UUID appId ) throws Exception {

        final ApplicationScope managementAppScope = getApplicationScope(CpNamingUtils.MANAGEMENT_APPLICATION_ID);
        final EntityCollectionManager managementCollectionManager = entityCollectionManagerFactory.createCollectionManager(managementAppScope);

        Observable<Edge> edgesObservable = getApplicationInfoEdges();
        //get the graph for all app infos
        Observable<org.apache.usergrid.persistence.model.entity.Entity> entityObs =
                edgesObservable.flatMap(edge -> {
                    final Id appInfoId = edge.getTargetNode();
                    return managementCollectionManager
                        .load(appInfoId)
                        .filter( entity ->{
                            //check for app id
                            return  entity != null ? entity.getId().getUuid().equals(appId) : false;
                        });
                });

        // don't expect many applications, so we block
        org.apache.usergrid.persistence.model.entity.Entity applicationInfo =
            entityObs.toBlocking().lastOrDefault(null);

        if ( applicationInfo == null ) {
            return null;
        }

        Class clazz = Schema.getDefaultSchema().getEntityClass(applicationInfo.getId().getType());

        Entity entity = EntityFactory.newEntity(
            applicationInfo.getId().getUuid(), applicationInfo.getId().getType(), clazz );

        entity.setProperties( CpEntityMapUtils.toMap( applicationInfo ) );

        return entity;

    }


    /**
     * Use Graph to get old appinfos from the old and deprecated System App.
     */
    public Observable<org.apache.usergrid.persistence.model.entity.Entity> getOldAppInfos( ) {

        final ApplicationScope systemAppScope = getApplicationScope(CpNamingUtils.SYSTEM_APP_ID);

        final EntityCollectionManager systemCollectionManager =
            entityCollectionManagerFactory.createCollectionManager(systemAppScope);

        final GraphManager gm = graphManagerFactory.createEdgeManager(systemAppScope);

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName("appinfos");

        Id rootAppId = systemAppScope.getApplication();

        final SimpleSearchByEdgeType simpleSearchByEdgeType =  new SimpleSearchByEdgeType(
            rootAppId, edgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, Optional.absent());

        Observable<org.apache.usergrid.persistence.model.entity.Entity> entityObs =
            gm.loadEdgesFromSource( simpleSearchByEdgeType )
            .flatMap(edge -> {
                final Id appInfoId = edge.getTargetNode();

                return systemCollectionManager.load(appInfoId)
                    .filter(entity -> (entity != null));
            });

        return entityObs;
    }

    Observable<Edge> edgesObservable;
    public Observable<Edge> getApplicationInfoEdges() {
        final ApplicationScope managementAppScope = getApplicationScope(CpNamingUtils.MANAGEMENT_APPLICATION_ID);
        final GraphManager gm = graphManagerFactory.createEdgeManager(managementAppScope);

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName(CpNamingUtils.APPLICATION_INFOS);


        final SimpleSearchByEdgeType simpleSearchByEdgeType =  new SimpleSearchByEdgeType(
            CpNamingUtils.generateApplicationId(CpNamingUtils.MANAGEMENT_APPLICATION_ID), edgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
            Optional.absent());
        edgesObservable = edgesObservable !=null ? edgesObservable : gm.loadEdgesFromSource( simpleSearchByEdgeType );
        return edgesObservable;
    }
}
