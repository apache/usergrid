/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.index;

import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class CollectionDeleteServiceImpl implements CollectionDeleteService {
    private static final Logger logger = LoggerFactory.getLogger(CollectionDeleteServiceImpl.class );

    private CollectionVersionManagerFactory collectionVersionManagerFactory;
    private AsyncEventService asyncEventService;

    @Inject
    public CollectionDeleteServiceImpl(
        final CollectionVersionManagerFactory collectionVersionManagerFactory,
        final AsyncEventService asyncEventService
    )
    {
        this.collectionVersionManagerFactory = collectionVersionManagerFactory;
        this.asyncEventService = asyncEventService;
    }

    @Override
    public void deleteCollection(final UUID applicationID, final String baseCollectionName) {
        CollectionScope scope = new CollectionScopeImpl(applicationID, baseCollectionName);
        CollectionVersionManager collectionVersionManager = collectionVersionManagerFactory.getInstance(scope);

        // change version
        String oldVersion = collectionVersionManager.updateCollectionVersion();

        // queue up delete of old version entities
        asyncEventService.queueCollectionDelete(scope, oldVersion);
    }

}
