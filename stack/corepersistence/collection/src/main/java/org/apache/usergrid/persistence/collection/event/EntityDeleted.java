/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.event;


import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 * Invoked when an entity is deleted.  The delete log entry is not removed until all instances of this listener has completed.
 * If any listener fails with an exception, the entity will not be removed.
 *
 */
public interface EntityDeleted {


    /**
     * The event fired when an entity is deleted
     *
     * @param scope The scope of the entity
     * @param entityId The id of the entity
     * @param version the entity version
     */
    public void deleted( final ApplicationScope scope, final Id entityId, final UUID version);

}
