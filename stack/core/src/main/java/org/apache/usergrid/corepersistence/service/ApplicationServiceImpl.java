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

import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import rx.Observable;

/**
 * implementation of application operations
 */
public class ApplicationServiceImpl  implements ApplicationService{

    private final AllEntityIdsObservable allEntityIdsObservable;
    private final AsyncEventService asyncEventService;
    private final EventBuilder eventBuilder;

    public ApplicationServiceImpl(AllEntityIdsObservable allEntityIdsObservable, AsyncEventService asyncEventService, EventBuilder eventBuilder){

        this.allEntityIdsObservable = allEntityIdsObservable;
        this.asyncEventService = asyncEventService;
        this.eventBuilder = eventBuilder;
    }
    @Override
    public Observable<Integer> deleteAllEntities(ApplicationScope applicationScope) {
        if(applicationScope.getApplication().getUuid().equals(CpNamingUtils.MANAGEMENT_APPLICATION_ID)){
            throw new IllegalArgumentException("Can't delete from management app");
        }

        //EventBuilder eventBuilder = injector.getInstance(EventBuilder.class);
        Observable appObservable  = Observable.just(applicationScope);


        Observable<Integer> countObservable = allEntityIdsObservable.getEntities(appObservable)
            //.map(entity -> eventBuilder.buildEntityDelete(applicationScope, entity.getId()).getEntitiesCompacted())
            .doOnNext(entity -> asyncEventService.queueEntityDelete(applicationScope, entity.getId()))
            .count();
        return countObservable;
    }
}
