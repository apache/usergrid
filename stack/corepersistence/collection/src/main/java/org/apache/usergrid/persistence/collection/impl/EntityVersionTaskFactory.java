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
package org.apache.usergrid.persistence.collection.impl;



import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.impl.EntityDeletedTask;
import org.apache.usergrid.persistence.collection.impl.EntityVersionCleanupTask;
import org.apache.usergrid.persistence.collection.impl.EntityVersionCreatedTask;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import java.util.UUID;


public interface EntityVersionTaskFactory {

    /**
     * Get a task for cleaning up latent entity data.  If includeVersion = true, the passed version will be cleaned up as well
     * Otherwise this is a V-1 operation
     *
     * @param scope
     * @param entityId
     * @param version
     * @param includeVersion
     * @return
     */
    public EntityVersionCleanupTask getCleanupTask( final CollectionScope scope, final Id entityId, final UUID version,
                                                    final boolean includeVersion );

    /**
     * Get an entityVersionCreatedTask
     * @param scope
     * @param entity
     * @return
     */
    public EntityVersionCreatedTask getCreatedTask( final CollectionScope scope, final Entity entity );

    /**
     * Get an entity deleted task
     * @param collectionScope
     * @param entityId
     * @param version
     * @return
     */
    public EntityDeletedTask getDeleteTask( final CollectionScope collectionScope, final Id entityId,
                                            final UUID version );

}
