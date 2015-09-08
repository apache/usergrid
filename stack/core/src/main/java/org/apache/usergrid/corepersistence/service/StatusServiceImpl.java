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
package org.apache.usergrid.corepersistence.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.util.StringUtils;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import rx.Observable;

import java.util.Map;
import java.util.UUID;

/**
 * Implementation for serializing job status or any kind
 */
public class StatusServiceImpl implements StatusService {

    private final MapManagerFactory mapManagerFactory;
    final static String statusKey = "status";
    final static String dataKey = "data";
    private final JsonFactory JSON_FACTORY = new JsonFactory();
    private final ObjectMapper MAPPER = new ObjectMapper( JSON_FACTORY );

    @Inject
    public StatusServiceImpl(final MapManagerFactory mapManagerFactory){
        this.mapManagerFactory = mapManagerFactory;
    }

    @Override
    public Observable<UUID> setStatus(final UUID applicationId, final UUID jobId, final Status status, final Map<String, Object> data) {
        return Observable.create(sub -> {
            final String jobString = StringUtils.sanitizeUUID(jobId);
            final Id appId = CpNamingUtils.generateApplicationId(applicationId);
            final MapManager mapManager = mapManagerFactory.createMapManager(new MapScopeImpl(appId, "status"));
            try {
                final String dataString = MAPPER.writeValueAsString(data);
                mapManager.putString(jobString + dataKey, dataString);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mapManager.putString(jobString + statusKey, status.toString());
            sub.onNext(jobId);
            sub.onCompleted();
        });
    }

    @Override
    public Observable<JobStatus> getStatus(final UUID applicationId, UUID jobId) {
        final String jobString = StringUtils.sanitizeUUID(jobId);

        return Observable.create(subscriber -> {
            Id appId = CpNamingUtils.generateApplicationId(applicationId);
            final MapManager mapManager = mapManagerFactory.createMapManager(new MapScopeImpl(appId, "status"));
            try {
                final Map<String, Object> data = MAPPER.readValue(mapManager.getString(jobString+dataKey), Map.class);
                final Status status = Status.valueOf(mapManager.getString(jobString+statusKey));
                subscriber.onNext(new JobStatus(jobId,status,data));
                subscriber.onCompleted();
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        });
    }
}
