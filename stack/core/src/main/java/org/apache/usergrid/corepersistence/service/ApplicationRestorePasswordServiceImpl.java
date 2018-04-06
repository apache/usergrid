/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.corepersistence.service;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import java.util.UUID;

public class ApplicationRestorePasswordServiceImpl implements ApplicationRestorePasswordService {
    private final MapManagerFactory mapManagerFactory;
    final static String restorePasswordNamespace = "appRestorePassword";
    final static String passwordKey = "password";

    @Inject
    public ApplicationRestorePasswordServiceImpl(final MapManagerFactory mapManagerFactory) {
        this.mapManagerFactory = mapManagerFactory;
    }

    private MapManager getMapManager(final UUID applicationId) {
        final Id appId = CpNamingUtils.generateApplicationId(applicationId);
        return mapManagerFactory.createMapManager(new MapScopeImpl(appId, restorePasswordNamespace));
    }

    @Override
    public String getApplicationRestorePassword(final UUID applicationId) {
        Preconditions.checkNotNull(applicationId, "app id is null");

        MapManager mapManager = getMapManager(applicationId);
        return mapManager.getString(passwordKey);

    }

    @Override
    public void setApplicationRestorePassword(final UUID applicationId, final String restorePassword) {
        Preconditions.checkNotNull(applicationId, "app id is null");
        Preconditions.checkArgument(!StringUtils.isEmpty(restorePassword), "restorePassword is empty");

        MapManager mapManager = getMapManager(applicationId);
        mapManager.putString(passwordKey, restorePassword);

    }

    @Override
    public void removeApplicationRestorePassword(final UUID applicationId) {
        Preconditions.checkNotNull(applicationId, "app id is null");

        MapManager mapManager = getMapManager(applicationId);
        mapManager.delete(passwordKey);
    }

}
