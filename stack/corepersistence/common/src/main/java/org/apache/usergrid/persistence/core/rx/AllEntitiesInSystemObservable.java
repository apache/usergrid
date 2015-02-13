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
package org.apache.usergrid.persistence.core.rx;

import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import rx.Observable;


/**
 * An observable that will emit every entity Id stored in our entire system across all apps.
 * Note that this only walks each application applicationId graph, and emits edges from the applicationId and it's edges as the s
 * source node
 */
public interface AllEntitiesInSystemObservable<T extends ApplicationScope> {
    /**
     * Return an observable that emits all entities in the system.
     *
     * @param bufferSize The amount of entityIds to buffer into each ApplicationEntityGroup.  Note that if we exceed the buffer size
     *                   you may be more than 1 ApplicationEntityGroup with the same application and different ids
     */
    public Observable<ApplicationEntityGroup<T>> getAllEntitiesInSystem(final int bufferSize);

    /**
     * Return an observable that emits all entities in the system.
     *
     * @param appIdObservable list of app ids
     * @param bufferSize The amount of entityIds to buffer into each ApplicationEntityGroup.  Note that if we exceed the buffer size
     *                   you may be more than 1 ApplicationEntityGroup with the same application and different ids
     */
    public Observable<ApplicationEntityGroup<T>> getAllEntitiesInSystem(Observable<Id> appIdObservable, final int bufferSize);


}

