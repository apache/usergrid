/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.api;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Servlet configuration information.
 */
@FigSingleton
public interface CoordinatorFig extends GuicyFig {
    String UPLOAD_PATH = "coordinator.endpoint.upload";
    String UPLOAD_PATH_DEFAULT = "/upload";
    @Key( UPLOAD_PATH )
    @Default( UPLOAD_PATH_DEFAULT )
    String getUploadPath();


    String STORE_RESULTS_PATH = "coordinator.endpoint.store.results";
    String STORE_RESULTS_PATH_DEFAULT = "/run/store";
    @Key( STORE_RESULTS_PATH )
    @Default( STORE_RESULTS_PATH_DEFAULT )
    String getStoreResultsPath();


    String RUN_STATUS_PATH = "coordinator.endpoint.run.status";
    String RUN_STATUS_PATH_DEFAULT = "/run/status";
    @Key( RUN_STATUS_PATH )
    @Default( RUN_STATUS_PATH_DEFAULT )
    String getRunStatusPath();


    String RUN_STATS_PATH = "coordinator.endpoint.run.stats";
    String RUN_STATS_PATH_DEFAULT = "/run/stats";
    @Key( RUN_STATS_PATH )
    @Default( RUN_STATS_PATH_DEFAULT )
    String getRunStatsPath();


    String RUN_NEXT_PATH = "coordinator.endpoint.run.next";
    String RUN_NEXT_PATH_DEFAULT = "/run/next";
    @Key( RUN_NEXT_PATH )
    @Default( RUN_NEXT_PATH_DEFAULT )
    String getRunNextPath();

    String RUN_COMPLETED_PATH = "coordinator.endpoint.run.completed";
    String RUN_COMPLETED_PATH_DEFAULT = "/run/completed";
    @Key( RUN_COMPLETED_PATH )
    @Default( RUN_COMPLETED_PATH_DEFAULT )
    String getRunCompletedPath();


    String RUNNERS_LIST_PATH = "coordinator.endpoint.runners.list";
    String RUNNERS_LIST_PATH_DEFAULT = "/runners/list";
    @Key( RUNNERS_LIST_PATH )
    @Default( RUNNERS_LIST_PATH_DEFAULT )
    String getRunnersListPath();


    String RUNNERS_REGISTER_PATH = "coordinator.endpoint.runners.register";
    String RUNNERS_REGISTER_PATH_DEFAULT = "/runners/register";
    @Key( RUNNERS_REGISTER_PATH )
    @Default( RUNNERS_REGISTER_PATH_DEFAULT )
    String getRunnersRegisterPath();


    String RUNNERS_UNREGISTER_PATH = "coordinator.endpoint.runners.unregister";
    String RUNNERS_UNREGISTER_PATH_DEFAULT = "/runners/unregister";
    @Key( RUNNERS_UNREGISTER_PATH )
    @Default( RUNNERS_UNREGISTER_PATH_DEFAULT )
    String getRunnersUnregisterPath();


    String ENDPOINT = "coordinator.endpoint";
    String ENDPOINT_DEFAULT = "https://localhost:8443";
    @Key( ENDPOINT )
    @Default( ENDPOINT_DEFAULT )
    String getEndpoint();


    String USERNAME = "coordinator.username";
    String USERNAME_DEFAULT = "user";
    @Key( USERNAME )
    @Default( USERNAME_DEFAULT )
    String getUsername();


    String PASSWORD = "coordinator.password";
    String PASSWORD_DEFAULT = "pass";
    @Key( PASSWORD )
    @Default( PASSWORD_DEFAULT )
    String getPassword();

    String PROPERTIES_PATH = "coordinator.endpoint.properties";
    String PROPERTIES_PATH_DEFAULT = "/properties";
    @Key( PROPERTIES_PATH )
    @Default( PROPERTIES_PATH_DEFAULT )
    String getPropertiesPath();

}
