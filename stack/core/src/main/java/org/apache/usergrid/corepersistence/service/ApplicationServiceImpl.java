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

import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import rx.Observable;

/**
 * implementation of application operations
 */
public class ApplicationServiceImpl  implements ApplicationService{

    private final AllEntityIdsObservable allEntityIdsObservable;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final AsyncEventService asyncEventService;
    private final EventBuilder eventBuilder;

    @Inject
    public ApplicationServiceImpl(AllEntityIdsObservable allEntityIdsObservable, EntityCollectionManagerFactory entityCollectionManagerFactory, AsyncEventService asyncEventService, EventBuilder eventBuilder){

        this.allEntityIdsObservable = allEntityIdsObservable;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.asyncEventService = asyncEventService;
        this.eventBuilder = eventBuilder;
    }
    @Override
    public Observable<Id> deleteAllEntities(ApplicationScope applicationScope) {
        if(applicationScope.getApplication().getUuid().equals(CpNamingUtils.MANAGEMENT_APPLICATION_ID)){
            throw new IllegalArgumentException("Can't delete from management app");
        }

        //EventBuilder eventBuilder = injector.getInstance(EventBuilder.class);
        Observable appObservable  = Observable.just(applicationScope);
        EntityCollectionManager entityCollectionManager = entityCollectionManagerFactory.createCollectionManager(applicationScope);


        Observable<Id> countObservable = allEntityIdsObservable.getEntities(appObservable)
            //.map(entity -> eventBuilder.buildEntityDelete(applicationScope, entity.getId()).getEntitiesCompacted())
            .map(entityIdScope -> ((EntityIdScope) entityIdScope).getId())
            .doOnNext(id ->
                    entityCollectionManager.mark((Id) id)
                        .doOnNext(id2 -> asyncEventService.queueEntityDelete(applicationScope, (Id) id2))
            );
        return countObservable;
    }
}
