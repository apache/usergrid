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
package org.apache.usergrid.chop.webapp.elasticsearch;


import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.internal.InternalNode;
import org.safehaus.guicyfig.GuicyFigModule;

import java.util.ArrayList;
import java.util.List;


/**
 * A class representing an embedded instance.
 */
@Singleton
public class EsEmbedded {
    private final ElasticSearchFig config;
    private InternalNode inode;
    private boolean started;


    public EsEmbedded() {
        List<Module> modules = new ArrayList<Module>(1);
        modules.add(new GuicyFigModule(ElasticSearchFig.class));
        config = Guice.createInjector(modules).getInstance(ElasticSearchFig.class);
    }


    @Inject
    public EsEmbedded(ElasticSearchFig config) {
        this.config = config;
    }


    public void start() {
        inode = EmbeddedUtils.newInstance(config);
        Client client = inode.client();
        client.admin().cluster().prepareHealth().setWaitForGreenStatus().setTimeout(
                TimeValue.timeValueSeconds(5)).execute().actionGet();
        started = true;
    }


    public void stop() {
        inode.close();
        started = false;
    }


    public ElasticSearchFig getConfig() {
        return config;
    }


    public boolean isStarted() {
        return started;
    }
}

