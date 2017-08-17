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

public class CollectionClearServiceImpl implements CollectionClearService {
    private static final Logger logger = LoggerFactory.getLogger(CollectionClearServiceImpl.class );

    private CollectionVersionManagerFactory collectionVersionManagerFactory;
    private AsyncEventService asyncEventService;

    @Inject
    public CollectionClearServiceImpl(
        final CollectionVersionManagerFactory collectionVersionManagerFactory,
        final AsyncEventService asyncEventService
    )
    {
        this.collectionVersionManagerFactory = collectionVersionManagerFactory;
        this.asyncEventService = asyncEventService;
    }

    @Override
    public void clearCollection(final UUID applicationID, final String baseCollectionName) {
        CollectionScope scope = new CollectionScopeImpl(applicationID, baseCollectionName);
        CollectionVersionManager collectionVersionManager = collectionVersionManagerFactory.getInstance(scope);

        // change version
        String oldVersion = collectionVersionManager.updateCollectionVersion();
        logger.info("Collection cleared: appID:{} baseCollectionName:{} oldVersion:{} newVersion:{}",
            applicationID.toString(), baseCollectionName, oldVersion, collectionVersionManager.getCollectionVersion(false));

        // queue up delete of old version entities
        asyncEventService.queueCollectionClear(scope, oldVersion);
    }

    @Override
    public String getCollectionVersion(UUID applicationID, String baseCollectionName) {
        CollectionScope scope = new CollectionScopeImpl(applicationID, baseCollectionName);
        CollectionVersionManager collectionVersionManager = collectionVersionManagerFactory.getInstance(scope);

        String currentVersion = collectionVersionManager.getCollectionVersion(true);

        return currentVersion;
    }

}
