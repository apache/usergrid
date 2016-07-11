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
package org.apache.usergrid.persistence.core.guice;


import org.apache.usergrid.persistence.core.guicyfig.ClusterFig;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Created by russo on 8/27/15.
 */
@Singleton
public class SettingsValidationCluster {

    @Inject
    public SettingsValidationCluster(final ClusterFig clusterFig) {

        final String configuredClusterName = clusterFig.getClusterName();
        final String defaultCluster = ClusterFig.VALIDATION_DEFAULT_VALUE;

        Preconditions.checkArgument(!configuredClusterName.equalsIgnoreCase(defaultCluster), ClusterFig.CLUSTER_NAME_PROPERTY + " property must be set.");

    }

}
