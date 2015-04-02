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
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.*;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.util.HashMap;
import java.util.Iterator;
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
        return 1; // standalone plugin, happens once
    }


    @Override
    public PluginPhase getPhase() {
        return PluginPhase.MIGRATE;
    }


    @Override
    public void run(ProgressObserver observer) {

        final int version = migrationInfoSerialization.getVersion( getName() );

        if ( version == getMaxVersion() ) {
            logger.debug("Skipping Migration Plugin: " + getName());
            return;
        }

        observer.start();

        // Get appinfos from the Graph, we don't expect many so use iterator

        final Iterator<org.apache.usergrid.persistence.model.entity.Entity> iterator =
            getOldAppInfos().toBlocking().getIterator();

        if ( iterator.hasNext() ) {

            // we found appinfos, now migrate them to application_infos in the Management App

            final EntityManager em = emf.getEntityManager( emf.getManagementAppId());

            String currentAppName = null;
            try {
                logger.info("Migrating old appinfos");

                while ( iterator.hasNext() ) {

                    Map oldAppInfo = CpEntityMapUtils.toMap( iterator.next() );
                    currentAppName = (String)oldAppInfo.get( PROPERTY_NAME );

                    migragrateOneAppInfo(em, oldAppInfo, observer);
                }

                // note that the old appinfos are not deleted

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


    private void migragrateOneAppInfo(
        EntityManager em, Map oldAppInfo, ProgressObserver observer) throws Exception {

        final String name = (String)oldAppInfo.get( PROPERTY_NAME );
        final String orgName = name.split("/")[0];
        final String appName = name.split("/")[1];

        UUID applicationId;

        Object uuidObject = oldAppInfo.get("applicationUuid");
        if (uuidObject instanceof UUID) {
            applicationId = (UUID) uuidObject;
        } else {
            applicationId = UUIDUtils.tryExtractUUID(uuidObject.toString());
        }

        // create and connect new APPLICATION_INFO oldAppInfo to Organization

        final UUID appId = applicationId;

        Entity appInfo = getApplicationInfo( emf, appId );
        if ( appInfo == null ) {
            Map<String, Object> appInfoMap = new HashMap<String, Object>() {{
                put(PROPERTY_NAME, name);
                put(PROPERTY_APPLICATION_ID, appId);
            }};
            appInfo = em.create(appId, CpNamingUtils.APPLICATION_INFO, appInfoMap);
            observer.update( getMaxVersion(), "Created application_info for " + appName);

        } else {
            appInfo.setProperty(PROPERTY_APPLICATION_ID, appId);
            em.update(appInfo);
            observer.update( getMaxVersion(), "Updated existing application_info for " + appName);
        }

        // create org->app connections, but not for apps in dummy "usergrid" internal organization

        if ( !orgName.equals("usergrid") ) {
            EntityRef orgRef = em.getAlias(Group.ENTITY_TYPE, orgName );
            em.createConnection(orgRef, "owns", appInfo);
        }
    }


    /**
     * TODO: Use Graph to get application_info for an specified Application.
     */
    private Entity getApplicationInfo( final EntityManagerFactory emf, final UUID appId ) throws Exception {

        final ApplicationScope appScope = getApplicationScope( emf.getManagementAppId() );

        final EntityCollectionManager collectionManager =
            entityCollectionManagerFactory.createCollectionManager( appScope );

        final GraphManager gm = graphManagerFactory.createEdgeManager(appScope);

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.APPLICATION_INFOS );

        Id rootAppId = appScope.getApplication();

        final SimpleSearchByEdgeType simpleSearchByEdgeType =  new SimpleSearchByEdgeType(
            rootAppId, edgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, null);

        // TODO: is there a better way?

        Observable<org.apache.usergrid.persistence.model.entity.Entity> entityObs =
            gm.loadEdgesFromSource( simpleSearchByEdgeType )
                .flatMap(new Func1<Edge, Observable<org.apache.usergrid.persistence.model.entity.Entity>>() {

                    @Override
                    public Observable<org.apache.usergrid.persistence.model.entity.Entity> call(final Edge edge) {

                        final Id appInfoId = edge.getTargetNode();

                        return collectionManager.load(appInfoId)
                            .filter(new Func1<org.apache.usergrid.persistence.model.entity.Entity, Boolean>() {
                                @Override
                                public Boolean call(final org.apache.usergrid.persistence.model.entity.Entity entity) {
                                    if (entity == null) {
                                        logger.warn("Encountered a null application info for id {}", appInfoId);
                                        return false;
                                    }
                                    if ( entity.getId().getUuid().equals( appId )) {
                                        return true;
                                    }
                                    return false;
                                }
                            });
                    }
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

//        UUID mgmtAppId = emf.getManagementAppId();
//        EntityManager rootEm = emf.getEntityManager( mgmtAppId );
//
//        final Results applicationInfoResults = rootEm.searchCollection(
//            new SimpleEntityRef("application", mgmtAppId), "application_infos",
//            Query.fromQL("select * where applicationId=" + appId.toString()));
//
//        return applicationInfoResults.getEntity();
    }


    /**
     * Use Graph to get old appinfos from the old and deprecated System App.
     */
    public Observable<org.apache.usergrid.persistence.model.entity.Entity> getOldAppInfos( ) {

        final ApplicationScope appScope = getApplicationScope( CpNamingUtils.SYSTEM_APP_ID );

        final EntityCollectionManager collectionManager =
            entityCollectionManagerFactory.createCollectionManager( appScope );

        final GraphManager gm = graphManagerFactory.createEdgeManager(appScope);

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( "appinfos" );

        Id rootAppId = appScope.getApplication();

        final SimpleSearchByEdgeType simpleSearchByEdgeType =  new SimpleSearchByEdgeType(
            rootAppId, edgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, null);

        Observable<org.apache.usergrid.persistence.model.entity.Entity> entityObs =
            gm.loadEdgesFromSource( simpleSearchByEdgeType )
            .flatMap(new Func1<Edge, Observable<org.apache.usergrid.persistence.model.entity.Entity>>() {

                @Override
                public Observable<org.apache.usergrid.persistence.model.entity.Entity> call(final Edge edge) {

                    final Id appInfoId = edge.getTargetNode();

                    return collectionManager.load(appInfoId)
                        .filter(new Func1<org.apache.usergrid.persistence.model.entity.Entity, Boolean>() {
                            @Override
                            public Boolean call(final org.apache.usergrid.persistence.model.entity.Entity entity) {
                                if (entity == null) {
                                    logger.warn("Encountered a null application info for id {}", appInfoId);
                                    return false;
                                }
                                return true;
                            }
                        });
                }
            });

        return entityObs;
    }
}
