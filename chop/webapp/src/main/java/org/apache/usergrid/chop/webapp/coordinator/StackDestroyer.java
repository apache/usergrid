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
package org.apache.usergrid.chop.webapp.coordinator;


import java.util.Collection;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.chop.api.ProviderParams;
import org.apache.usergrid.chop.api.store.amazon.AmazonFig;
import org.apache.usergrid.chop.spi.InstanceManager;
import org.apache.usergrid.chop.stack.CoordinatedStack;
import org.apache.usergrid.chop.stack.ICoordinatedCluster;
import org.apache.usergrid.chop.stack.Instance;
import org.apache.usergrid.chop.stack.SetupStackSignal;
import org.apache.usergrid.chop.webapp.dao.ProviderParamsDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;

import com.google.inject.Inject;


public class StackDestroyer {

    private static final Logger LOG = LoggerFactory.getLogger( StackDestroyer.class );

    @Inject
    private ProviderParamsDao providerParamsDao;

    private CoordinatedStack stack;


    public StackDestroyer( CoordinatedStack stack ) {
        this.stack = stack;
    }


    public CoordinatedStack getStack() {
        return stack;
    }


    public void destroy() {
        providerParamsDao = InjectorFactory.getInstance( ProviderParamsDao.class );

        ProviderParams providerParams = providerParamsDao.getByUser( stack.getUser().getUsername() );

        /** Bypass the keys in AmazonFig so that it uses the ones belonging to the user */
        AmazonFig amazonFig = InjectorFactory.getInstance( AmazonFig.class );
        amazonFig.bypass( AmazonFig.AWS_ACCESS_KEY, providerParams.getAccessKey() );
        amazonFig.bypass( AmazonFig.AWS_SECRET_KEY, providerParams.getSecretKey() );

        InstanceManager instanceManager = InjectorFactory.getInstance( InstanceManager.class );
        instanceManager.setDataCenter( stack.getDataCenter() );

        Collection<String> instances = new LinkedList<String>();
        for( Instance instance: stack.getRunnerInstances() ) {
            instances.add( instance.getId() );
        }
        for( ICoordinatedCluster cluster: stack.getClusters() ) {
            for( Instance instance: cluster.getInstances() ) {
                instances.add( instance.getId() );
            }
        }
        LOG.info( "Destroying all {} cluster and runner instances of {} stack...", instances.size(), stack.getName() );
        instanceManager.terminateInstances( instances );
        LOG.info( "Destroyed {} stack.", stack.getName() );
    }
}
