/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection;


import org.apache.usergrid.persistence.core.scope.ApplicationScope;


/**
 * A basic factory that creates a collection manager with the given context.
 * Each instance of this factory should exist for a Single ApplicationScope
 */
public interface EntityCollectionManagerFactory {

    /**
     * Create a new EntityCollectionManager for the given context.
     * The EntityCollectionManager can safely be used on the current thread
     * and will shard responses.  The returned instance should not be shared
     * among threads it will not be guaranteed to be thread safe.
     *
     * @param applicationScope The applicationScope to use
     * when creating the EntityCollectionManager
     *
     * @return The EntityCollectionManager to perform operations within the applicationscope provided
     */
    EntityCollectionManager
        createCollectionManager( ApplicationScope applicationScope );


    void invalidate();
}
