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


import java.util.List;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 * Invoked when an entity version is removed.  Note that this is not a deletion of the entity
 * itself, only the version itself.
 *
 */
public interface EntityVersionDeleted {

    /**
     * The version specified was removed.
     *
     * @param scope The scope of the entity
     * @param entityId The entity Id that was removed
     * @param entityVersions The versions that are to be removed
     */
    public void versionDeleted(final ApplicationScope scope, final Id entityId,
            final List<MvccLogEntry> entityVersions);

}
