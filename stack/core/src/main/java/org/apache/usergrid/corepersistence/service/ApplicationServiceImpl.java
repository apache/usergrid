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
package org.apache.usergrid.corepersistence.service;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.corepersistence.index.CollectionSettings;
import org.apache.usergrid.corepersistence.index.CollectionSettingsFactory;
import org.apache.usergrid.corepersistence.index.CollectionSettingsScopeImpl;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.utils.InflectionUtils;
import rx.Observable;

import java.util.ArrayList;
import java.util.Map;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createGraphOperationTimestamp;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;

/**
 * implementation of application operations
 */
public class ApplicationServiceImpl  implements ApplicationService{

    private final AllEntityIdsObservable allEntityIdsObservable;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final AsyncEventService asyncEventService;
    private final EventBuilder eventBuilder;
    private final MapManagerFactory mapManagerFactory;
    private final GraphManagerFactory graphManagerFactory;
    private final CollectionSettingsFactory collectionSettingsFactory;
    private final ActorSystemFig actorSystemFig;


    @Inject
    public ApplicationServiceImpl(AllEntityIdsObservable allEntityIdsObservable,
                                  EntityCollectionManagerFactory entityCollectionManagerFactory,
                                  AsyncEventService asyncEventService,
                                  EventBuilder eventBuilder,
                                  MapManagerFactory mapManagerFactory,
                                  GraphManagerFactory graphManagerFactory,
                                  CollectionSettingsFactory collectionSettingsFactory,
                                  ActorSystemFig actorSystemFig
    ){

        this.allEntityIdsObservable = allEntityIdsObservable;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.asyncEventService = asyncEventService;
        this.eventBuilder = eventBuilder;
        this.mapManagerFactory = mapManagerFactory;
        this.graphManagerFactory = graphManagerFactory;
        this.collectionSettingsFactory = collectionSettingsFactory;
        this.actorSystemFig = actorSystemFig;
    }


    @Override
    public Observable<Id> deleteAllEntities(final ApplicationScope applicationScope, final int limit) {
        if (applicationScope.getApplication().getUuid().equals(CpNamingUtils.MANAGEMENT_APPLICATION_ID)) {
            throw new IllegalArgumentException("Can't delete from management app");
        }

        //EventBuilder eventBuilder = injector.getInstance(EventBuilder.class);
        Observable appObservable = Observable.just(applicationScope);
        EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager(applicationScope);
        GraphManager graphManager = graphManagerFactory.createEdgeManager(applicationScope);
        MapManager mapManager = getMapManagerForTypes(applicationScope);

        Observable<Id> countObservable = allEntityIdsObservable.getEntities(appObservable)
            .map(entityIdScope -> ((EntityIdScope) entityIdScope).getId())
            .filter(id -> {
                final String type = InflectionUtils.pluralize(((Id) id).getType());
                return ! (type.equals(Schema.COLLECTION_USERS)
                    || type.equals(Schema.COLLECTION_GROUPS)
                    || type.equals(InflectionUtils.pluralize( Schema.TYPE_APPLICATION))
                    || type.equals(Schema.COLLECTION_ROLES));
            })//skip application entity and users and groups and roles
            ;

        if(limit>0){
            countObservable = countObservable.limit(limit);
        }

        countObservable = countObservable.map(id -> {
            entityCollectionManager.mark((Id) id, null )
                .mergeWith(graphManager.markNode((Id) id, createGraphOperationTimestamp())).toBlocking().last();
            return id;
        })
            .doOnNext(id -> deleteAsync(mapManager, applicationScope, (Id) id));
        return countObservable;
    }


    /**
     * 4. Delete all entity documents out of elasticsearch.
     * 5. Compact Graph so that it deletes the marked values.
     * 6. Delete entity from cassandra using the map manager.
     **/
    private Id deleteAsync(MapManager mapManager, ApplicationScope applicationScope, Id entityId )  {
        try {
            //Step 4 && 5

            if ( !skipIndexingForType( entityId.getType(), applicationScope ) ) {

                asyncEventService.queueEntityDelete(applicationScope, entityId);
            }
            //Step 6
            //delete from our UUID index
            mapManager.delete(entityId.getUuid().toString());
            return entityId;
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    private boolean skipIndexingForType( String type, ApplicationScope applicationScope ) {

        boolean skipIndexing = false;

        String collectionName = Schema.defaultCollectionName( type );

        CollectionSettings collectionSettings = collectionSettingsFactory
            .getInstance( new CollectionSettingsScopeImpl(applicationScope.getApplication(), collectionName));

        Optional<Map<String, Object>> collectionIndexingSchema =
            collectionSettings.getCollectionSettings( collectionName );

        if ( collectionIndexingSchema.isPresent()) {
            Map jsonMapData = collectionIndexingSchema.get();
            final ArrayList fields = (ArrayList) jsonMapData.get( "fields" );
            if ( fields.size() == 1 && fields.get(0).equals("none")) {
                skipIndexing = true;
            }
        }

        return skipIndexing;
    }

    /**
     * Get the map manager for uuid mapping
     */
    private MapManager getMapManagerForTypes( ApplicationScope applicationScope) {
        Id mapOwner = new SimpleId( applicationScope.getApplication().getUuid(), TYPE_APPLICATION );

        final MapScope ms = CpNamingUtils.getEntityTypeMapScope(mapOwner);

        MapManager mm = mapManagerFactory.createMapManager(ms);

        return mm;
    }
}
