/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.corepersistence;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;


/**
 * Interface for classes that need to visit all collections, connections and entities.
 */
public interface CpVisitor {

    /**
     * Visit the entity as we're walking the structure
     * @param em
     * @param collName
     * @param visitedEntity
     */
    public void visitCollectionEntry( 
        EntityManager em, String collName, Entity visitedEntity );
}
