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


import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * Invoked after a new version of an entity has been created.
 * The entity should be a complete view of the entity.
 */
public interface EntityVersionCreated {

    /**
     * The new version of the entity. Note that this should be a fully merged view of the entity.
     * In the case of partial updates, the passed entity should be fully merged with it's previous
     * entries.
     * @param scope The scope of the entity
     * @param entity The fully loaded and merged entity
     */
    public void versionCreated( final ApplicationScope scope, final Entity entity );
}
