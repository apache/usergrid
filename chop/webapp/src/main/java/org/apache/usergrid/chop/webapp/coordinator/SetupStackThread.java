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


import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.builder.HashCodeBuilder;

import org.apache.usergrid.chop.api.ProviderParams;
import org.apache.usergrid.chop.api.store.amazon.AmazonFig;
import org.apache.usergrid.chop.spi.InstanceManager;
import org.apache.usergrid.chop.spi.IpRuleManager;
import org.apache.usergrid.chop.spi.LaunchResult;
import org.apache.usergrid.chop.stack.BasicInstanceSpec;
import org.apache.usergrid.chop.stack.CoordinatedStack;
import org.apache.usergrid.chop.stack.ICoordinatedCluster;
import org.apache.usergrid.chop.stack.Instance;
import org.apache.usergrid.chop.stack.SetupStackSignal;
import org.apache.usergrid.chop.webapp.ChopUiFig;
import org.apache.usergrid.chop.webapp.dao.ProviderParamsDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;

import com.google.inject.Inject;


/** Encapsulates a CoordinatedStack and sets it up asynchronously */
public class SetupStackThread implements Callable<CoordinatedStack> {

    private static final Logger LOG = LoggerFactory.getLogger( SetupStackThread.class );

    @Inject
    private ChopUiFig chopUiFig;

    @Inject
    private ProviderParamsDao providerParamsDao;

    private CoordinatedStack stack;
    private String errorMessage;


    public SetupStackThread( CoordinatedStack stack ) {
        this.stack = stack;
    }


    public CoordinatedStack getStack() {
        return stack;
    }


    public String getErrorMessage() {
        return errorMessage;
    }


    @Override
    public CoordinatedStack call() {

        String keyFile;
        LinkedList<String> launchedInstances = new LinkedList<String>();

        providerParamsDao = InjectorFactory.getInstance( ProviderParamsDao.class );
        chopUiFig = InjectorFactory.getInstance( ChopUiFig.class );

        ProviderParams providerParams = providerParamsDao.getByUser( stack.getUser().getUsername() );

        /** Bypass the keys in AmazonFig so that it uses the ones belonging to the user */
        AmazonFig amazonFig = InjectorFactory.getInstance( AmazonFig.class );
        amazonFig.bypass( AmazonFig.AWS_ACCESS_KEY, providerParams.getAccessKey() );
        amazonFig.bypass( AmazonFig.AWS_SECRET_KEY, providerParams.getSecretKey() );

        InstanceManager instanceManager = InjectorFactory.getInstance( InstanceManager.class );
        IpRuleManager ipRuleManager = InjectorFactory.getInstance( IpRuleManager.class );

        File runnerJar = CoordinatorUtils.getRunnerJar( chopUiFig.getContextPath(), stack );

        ipRuleManager.setDataCenter( stack.getDataCenter() );
        ipRuleManager.applyIpRuleSet( stack.getIpRuleSet() );

        /** Setup clusters */
        for ( ICoordinatedCluster cluster : stack.getClusters() ) {
            LOG.info( "Starting setting up cluster {}...", cluster.getName() );
            keyFile = providerParams.getKeys().get( cluster.getInstanceSpec().getKeyName() );
            if ( keyFile == null ) {
                errorMessage = "No key found with name " + cluster.getInstanceSpec().getKeyName() +
                        " for cluster " + cluster.getName();
                LOG.warn( errorMessage + ", aborting and terminating launched instances..." );
                instanceManager.terminateInstances( launchedInstances );
                stack.setSetupState( SetupStackSignal.FAIL );
                stack.notifyAll();
                return null;
            }
            if ( !( new File( keyFile ) ).exists() ) {
                errorMessage = "Key file " + keyFile + " for cluster " + cluster.getName() + " not found";
                LOG.warn( errorMessage + ", aborting and terminating launched instances..." );
                instanceManager.terminateInstances( launchedInstances );
                stack.setSetupState( SetupStackSignal.FAIL );
                stack.notifyAll();
                return null;
            }

            LaunchResult result = instanceManager.launchCluster( stack, cluster,
                    chopUiFig.getLaunchClusterTimeout() );

            for ( Instance instance : result.getInstances() ) {
                launchedInstances.add( instance.getId() );
                cluster.add( instance );
            }

            /** Setup system properties, deploy the scripts and execute them on cluster instances */
            boolean success = false;
            try {
                success = CoordinatorUtils.executeClusterSSHCommands( cluster, runnerJar, keyFile );
            }
            catch ( Exception e ) {
                LOG.warn( "Error while executing SSH commands", e );
            }
            if ( ! success ) {
                errorMessage = "SSH commands have failed, will not continue";
                instanceManager.terminateInstances( launchedInstances );
                stack.setSetupState( SetupStackSignal.FAIL );
                stack.notifyAll();
                return null;
            }
            LOG.info( "Cluster {} is ready, moving on...", cluster.getName() );
        }

        /** Setup runners */
        LOG.info( "Starting setting up runner instances..." );
        keyFile = providerParams.getKeys().get( providerParams.getKeyName() );
        if ( keyFile == null ) {
            errorMessage = "No key found with name " + providerParams.getKeyName() + " for runners";
            LOG.warn( errorMessage + ", aborting and terminating launched instances..." );
            instanceManager.terminateInstances( launchedInstances );
            stack.setSetupState( SetupStackSignal.FAIL );
            stack.notifyAll();
            return null;
        }
        if ( ! ( new File( keyFile ) ).exists() ) {
            errorMessage = "Key file " + keyFile + " for runners not found";
            LOG.warn( errorMessage + ", aborting and terminating launched instances..." );
            instanceManager.terminateInstances( launchedInstances );
            stack.setSetupState( SetupStackSignal.FAIL );
            stack.notifyAll();
            return null;
        }
        BasicInstanceSpec runnerSpec = new BasicInstanceSpec();
        runnerSpec.setImageId( providerParams.getImageId() );
        runnerSpec.setType( providerParams.getInstanceType() );
        runnerSpec.setKeyName( providerParams.getKeyName() );

        LaunchResult result = instanceManager.launchRunners( stack, runnerSpec,
                chopUiFig.getLaunchClusterTimeout() );

        for ( Instance instance : result.getInstances() ) {
            launchedInstances.add( instance.getId() );
            stack.addRunnerInstance( instance );
        }

        /** Deploy and start runner.jar on instances */
        boolean success = false;
        try {
            success = CoordinatorUtils.executeRunnerSSHCommands( stack, runnerJar, keyFile );
        }
        catch ( Exception e ) {
            LOG.warn( "Error while executing SSH commands", e );
        }
        if ( ! success ) {
            errorMessage = "SSH commands have failed, will not continue";
            instanceManager.terminateInstances( launchedInstances );
            stack.setSetupState( SetupStackSignal.FAIL );
            stack.notifyAll();
            return null;
        }

        stack.setSetupState( SetupStackSignal.COMPLETE );
        LOG.info( "Stack {} is set up and ready...", stack.getName() );

        stack.notifyAll();
        return stack;
    }


    @Override
    public int hashCode() {
        if( errorMessage != null ) {
            return new HashCodeBuilder( 97, 71 )
                    .append( errorMessage )
                    .toHashCode();
        }
        return new HashCodeBuilder( 97, 71 )
                .append( stack.getId().toString() )
                .append( stack.getUser().getUsername() )
                .append( stack.getCommit().getId() )
                .append( stack.getModule().getId() )
                .append( stack.getRunnerCount() )
                .toHashCode();
    }


    @Override
    public boolean equals( final Object obj ) {
        if( this == obj ) {
            return true;
        }
        return obj != null &&
                obj instanceof SetupStackThread &&
                obj.hashCode() == this.hashCode();
    }
}
