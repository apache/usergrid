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
package org.apache.usergrid.chop.api.store.amazon;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.usergrid.chop.stack.BasicInstance;
import org.apache.usergrid.chop.stack.ICoordinatedCluster;
import org.apache.usergrid.chop.stack.ICoordinatedStack;
import org.apache.usergrid.chop.stack.Instance;
import org.apache.usergrid.chop.spi.InstanceManager;
import org.apache.usergrid.chop.stack.InstanceState;
import org.apache.usergrid.chop.spi.LaunchResult;
import org.apache.usergrid.chop.stack.BasicInstanceSpec;
import org.apache.usergrid.chop.stack.InstanceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.google.inject.Inject;


/** Implements all InstanceManager functionality for AmazonAWS  */
public class EC2InstanceManager implements InstanceManager {

    private static Logger LOG = LoggerFactory.getLogger( EC2InstanceManager.class );

    private static final long SLEEP_LENGTH = 3000;

    private AmazonEC2Client client;


    /**
     * @param amazonFig Fig object containing AWS credentials
     */
    @Inject
    public EC2InstanceManager( AmazonFig amazonFig ) {
        client = AmazonUtils.getEC2Client( amazonFig.getAwsAccessKey(), amazonFig.getAwsSecretKey() );
    }


    @Override
    public long getDefaultTimeout() {
        return SLEEP_LENGTH;
    }


    /**
     * Terminates instances with given Ids
     *
     * @param instanceIds
     */
    @Override
    public void terminateInstances( final Collection<String> instanceIds ) {
        if( instanceIds == null || instanceIds.size() == 0 ) {
            return;
        }
        TerminateInstancesRequest request = ( new TerminateInstancesRequest() ).withInstanceIds( instanceIds );
        client.terminateInstances( request );
    }


    /**
     * All public methods except <code>terminateInstances</code> use supplied arguments
     * to set the appropriate data center. So this is only needed before calling <code>terminateInstances</code>.
     *
     * @param dataCenter    Ec2Client's endpoint, us-east-1, us-west-2 etc.
     */
    @Override
    public void setDataCenter( final String dataCenter ) {
        client.setEndpoint( AmazonUtils.getEndpoint( dataCenter ) );
    }


    /**
     * Launches instances of given cluster.
     *
     * After launching instances, blocks for maximum <code>timeout</code> amount until all
     * instances get into the Running state.
     *
     * @param stack     <code>ICoordinatedStack</code> object containing the <code>cluster</code>
     * @param cluster
     * @param timeout   in milliseconds, if smaller than <code>getDefaultTimeout()</code> it doesn't wait
     * @return          resulting runner instances which successfully got in Running state
     */
    @Override
    public LaunchResult launchCluster( ICoordinatedStack stack, ICoordinatedCluster cluster, int timeout ) {

        RunInstancesResult runInstancesResult = null;
        try {
            RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

            runInstancesRequest.withImageId( cluster.getInstanceSpec().getImageId() )
                               .withInstanceType( cluster.getInstanceSpec().getType() )
                               .withMinCount( cluster.getSize() ).withMaxCount( cluster.getSize() )
                               .withKeyName( cluster.getInstanceSpec().getKeyName() )
                               .withSecurityGroups( stack.getIpRuleSet().getName() );

            if ( stack.getDataCenter() != null && !stack.getDataCenter().isEmpty() ) {
                runInstancesRequest = runInstancesRequest.withPlacement( new Placement( stack.getDataCenter() ) );
                client.setEndpoint( AmazonUtils.getEndpoint( stack.getDataCenter() ) );
            }

            runInstancesResult = client.runInstances( runInstancesRequest );
        }
        catch ( Exception e ) {
            LOG.error( "Error while launching cluster instances.", e );
            return new EC2LaunchResult( cluster.getInstanceSpec(), Collections.EMPTY_LIST );
        }

        LOG.info( "Created instances, setting the names now..." );

        List<String> instanceIds = new ArrayList<String>( cluster.getSize() );

        String instanceNames = getInstanceName( stack, cluster );

        int i = 0;
        for( com.amazonaws.services.ec2.model.Instance instance : runInstancesResult.getReservation().getInstances() ) {

            try {
                instanceIds.add( i, instance.getInstanceId() );
                LOG.debug( "Setting name of cluster instance with id: {}", instanceIds.get( i ) );

                List<Tag> tags = new ArrayList<Tag>();

                Tag t = new Tag();
                t.setKey( "Name" );
                t.setValue( instanceNames );
                tags.add( t );

                CreateTagsRequest ctr = new CreateTagsRequest();
                ctr.setTags( tags );
                ctr.withResources( instanceIds.get( i ) );
                client.createTags( ctr );
            }
            catch ( Exception e ) {
                LOG.warn( "Error while setting names", e );
            }
            i++;
        }

        LOG.info( "Names of the instances are set" );

        if ( timeout > SLEEP_LENGTH ) {
            LOG.info( "Waiting for maximum {} msec until all instances are running", timeout );
            boolean stateCheck = waitUntil( instanceIds, InstanceState.Running, timeout );

            if ( ! stateCheck ) {
                LOG.warn( "Waiting for instances to get into Running state has timed out" );
            }
        }

        Collection<Instance> instances = toInstances( getEC2Instances( instanceIds ) );

        return new EC2LaunchResult( cluster.getInstanceSpec(), instances );
    }


    /**
     * Launches runner instances of given stack.
     *
     * Given <code>ICoordinatedStack</code> and an <code>InstanceSpec</code>
     * defining its runners' instance specifications, launches all runner instances.
     * After launching instances, blocks for maximum <code>timeout</code> amount until all
     * instances get into the Running state.
     *
     * @param stack
     * @param spec
     * @param timeout   in milliseconds, if smaller than <code>getDefaultTimeout()</code> it doesn't wait
     * @return          resulting runner instances which successfully got in Running state
     */
    @Override
    public LaunchResult launchRunners( ICoordinatedStack stack, InstanceSpec spec, int timeout ) {

        RunInstancesResult runInstancesResult = null;
        try {
            RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

            runInstancesRequest.withImageId( spec.getImageId() ).withInstanceType( spec.getType() )
                               .withMinCount( stack.getRunnerCount() ).withMaxCount( stack.getRunnerCount() )
                               .withKeyName( spec.getKeyName() ).withSecurityGroups( stack.getIpRuleSet().getName() );

            if ( stack.getDataCenter() != null && !stack.getDataCenter().isEmpty() ) {
                runInstancesRequest = runInstancesRequest.withPlacement( new Placement( stack.getDataCenter() ) );
                client.setEndpoint( AmazonUtils.getEndpoint( stack.getDataCenter() ) );
            }

            runInstancesResult = client.runInstances( runInstancesRequest );
        }
        catch ( Exception e ) {
            LOG.error( "Error while launching runner instances.", e );
            return new EC2LaunchResult( spec, Collections.EMPTY_LIST );
        }

        LOG.info( "Created instances, setting the names now..." );

        List<String> instanceIds = new ArrayList<String>( stack.getRunnerCount() );
        String runnerNames = getRunnerName( stack );

        int i = 0;
        for( com.amazonaws.services.ec2.model.Instance instance : runInstancesResult.getReservation().getInstances() ) {

            try {
                instanceIds.add( i, instance.getInstanceId() );
                LOG.debug( "Setting name of runner instance with id: {}", instanceIds.get( i ) );

                List<Tag> tags = new ArrayList<Tag>();

                Tag t = new Tag();
                t.setKey( "Name" );
                t.setValue( runnerNames );
                tags.add( t );

                CreateTagsRequest ctr = new CreateTagsRequest();
                ctr.setTags( tags );
                ctr.withResources( instanceIds.get( i ) );
                client.createTags( ctr );
            }
            catch ( Exception e ) {
                LOG.warn( "Error while setting names", e );
            }
            i++;
        }

        LOG.info( "Names of the instances are set" );

        if ( timeout > SLEEP_LENGTH ) {
            LOG.info( "Waiting for maximum {} msec until all instances are running", timeout );
            boolean stateCheck = waitUntil( instanceIds, InstanceState.Running, timeout );

            if ( ! stateCheck ) {
                LOG.warn( "Waiting for instances to get into Running state has timed out" );
            }
        }

        Collection<Instance> instances = toInstances( getEC2Instances( instanceIds ) );

        return new EC2LaunchResult( spec, instances );
    }


    /**
     * @param stack     <code>ICoordinatedStack</code> object containing the <code>cluster</code>
     * @param cluster
     * @return          Cluster instances which are in <code>Running</code> state
     */
    @Override
    public Collection<Instance> getClusterInstances( ICoordinatedStack stack, ICoordinatedCluster cluster ) {

        String name = getInstanceName( stack, cluster );

        if( stack.getDataCenter() != null && ! stack.getDataCenter().isEmpty() ) {
            client.setEndpoint( AmazonUtils.getEndpoint( stack.getDataCenter() ) );
        }

        return toInstances( getEC2Instances( name, InstanceStateName.Running ) );

    }


    /**
     * @param stack
     * @return      Runner instances which belong to <code>stack</code> and in <code>Running</code> state
     */
    @Override
    public Collection<Instance> getRunnerInstances( ICoordinatedStack stack ) {

        String name = getRunnerName( stack );

        if( stack.getDataCenter() != null && ! stack.getDataCenter().isEmpty() ) {
            client.setEndpoint( AmazonUtils.getEndpoint( stack.getDataCenter() ) );
        }

        return toInstances( getEC2Instances( name, InstanceStateName.Running ) );

    }


    /**
     * @param name  Causes the method to return instances with given Name tag, give null if you want to get
     *              instances with all names
     * @param state Causes the method to return instances with given state, give null if you want to get instances in
     *              all states
     * @return      all instances that satisfy given parameters
     */
    protected Collection<com.amazonaws.services.ec2.model.Instance> getEC2Instances( String name,
                                                                               InstanceStateName state ) {

        Collection<com.amazonaws.services.ec2.model.Instance> instances =
                new LinkedList<com.amazonaws.services.ec2.model.Instance>();

        DescribeInstancesRequest request = new DescribeInstancesRequest();

        if ( name != null ) {

            List<String> valuesT1 = new ArrayList<String>();
            valuesT1.add( name );
            Filter filter = new Filter("tag:Name", valuesT1);
            request = request.withFilters( filter );

        }

        if ( state != null ) {

            List<String> valuesT1 = new ArrayList<String>();
            valuesT1.add( state.toString() );
            Filter filter = new Filter( "instance-state-name", valuesT1 );
            request = request.withFilters( filter );

        }

        DescribeInstancesResult result = null;
        try {
            result = client.describeInstances( request );
        }
        catch ( Exception e ) {
            LOG.error( "Error while getting instance information from AWS.", e );
            return Collections.EMPTY_LIST;
        }

        for ( Reservation reservation : result.getReservations() ) {
            for ( com.amazonaws.services.ec2.model.Instance in : reservation.getInstances() ) {
                instances.add( in );
            }
        }

        return instances;
    }


    /**
     * Queries instances with given Ids on AWS
     *
     * @param instanceIds   List of instance IDs
     * @return
     */
    protected Collection<com.amazonaws.services.ec2.model.Instance> getEC2Instances( Collection<String> instanceIds ) {
        if( instanceIds == null || instanceIds.size() == 0 ) {
            return new ArrayList<com.amazonaws.services.ec2.model.Instance>();
        }

        Collection<com.amazonaws.services.ec2.model.Instance> instances =
                new LinkedList<com.amazonaws.services.ec2.model.Instance>();

        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request = request.withInstanceIds( instanceIds );

        DescribeInstancesResult result = null;
        try {
            result = client.describeInstances( request );
        }
        catch ( Exception e ) {
            LOG.error( "Error while getting instance information from AWS.", e );
            return Collections.EMPTY_LIST;
        }

        for ( Reservation reservation : result.getReservations() ) {
            for ( com.amazonaws.services.ec2.model.Instance in : reservation.getInstances() ) {
                instances.add( in );
            }
        }

        return instances;
    }


    /**
     * Takes a collection of AWS instances, and converts them into a collection of <code>Instance</code>s
     *
     * @param ec2s
     * @return
     */
    protected Collection<Instance> toInstances( Collection<com.amazonaws.services.ec2.model.Instance> ec2s ) {
        Collection<Instance> instances = new ArrayList<Instance>( ec2s.size() );

        for( com.amazonaws.services.ec2.model.Instance ec2 : ec2s ) {
            instances.add( toInstance( ec2 ) );
        }

        return instances;
    }


    /**
     * Constructs and returns an BasicInstance object, using information from <code>ec2</code>
     *
     * @param ec2
     * @return
     */
    protected static Instance toInstance( com.amazonaws.services.ec2.model.Instance ec2 ) {
        Instance instance;
        BasicInstanceSpec spec;

        spec = new BasicInstanceSpec();
        spec.setImageId( ec2.getImageId() );
        spec.setKeyName( ec2.getKeyName() );
        spec.setType( ec2.getInstanceType() );

        instance = new BasicInstance(
                        ec2.getInstanceId(),
                        spec,
                        InstanceState.fromValue( ec2.getState().getName() ),
                        ec2.getPrivateDnsName(),
                        ec2.getPublicDnsName(),
                        ec2.getPrivateIpAddress(),
                        ec2.getPublicIpAddress()
                );

        return instance;
    }


    /**
     * Checks the state of all given instances in SLEEP_LENGTH intervals, returns when all instances are in expected
     * state or state check times out
     *
     * @param instanceIds   List of instance IDs whose states are going to be checked
     * @param state         Expected state to check
     * @param timeout       Timeout length in milliseconds
     * @return              true if all instances are in given state, false if timeout occured
     */
    public boolean waitUntil ( Collection<String> instanceIds, InstanceState state,  int timeout ) {

        List<String> instanceIdCopy = new ArrayList<String>( instanceIds );
        Calendar cal = Calendar.getInstance();
        cal.setTime( new Date() );
        long startTime = cal.getTimeInMillis();
        long timePassed;
        String stateStr;

        do {
            DescribeInstancesRequest dis = ( new DescribeInstancesRequest() ).withInstanceIds( instanceIdCopy );
            DescribeInstancesResult disresult = client.describeInstances( dis );
            // Since the request is filtered with instance IDs, there is always only one Reservation object
            Reservation reservation  = disresult.getReservations().iterator().next();
            for ( com.amazonaws.services.ec2.model.Instance in : reservation.getInstances() ) {

                stateStr = in.getState().getName();
                LOG.info( "{} is {}", in.getInstanceId(), in.getState().getName() );


                /** If expected state is ShuttingDown, also accept the Terminated ones */
                if( state == InstanceState.ShuttingDown ) {

                    if ( stateStr.equals( state.toString() ) ||
                            stateStr.equals( InstanceState.Terminated.toString() ) ) {

                        instanceIdCopy.remove( in.getInstanceId() );
                    }

                }
                /** If expected state is Pending, also accept the Running ones */
                else if( state == InstanceState.Pending ) {

                    if ( stateStr.equals( state.toString() ) ||
                            stateStr.equals( InstanceState.Running.toString() ) ) {

                        instanceIdCopy.remove( in.getInstanceId() );
                    }

                }
                /** If expected state is Stopping, also accept the Stopped ones */
                else if( state == InstanceState.Stopping ) {

                    if ( stateStr.equals( state.toString() ) ||
                            stateStr.equals( InstanceState.Stopped.toString() ) ) {

                        instanceIdCopy.remove( in.getInstanceId() );
                    }

                }
                else {
                    if ( in.getState().getName().equals( state.toString() ) ) {
                        instanceIdCopy.remove( in.getInstanceId() );
                    }
                }
            }
            cal.setTime( new Date() );
            timePassed = cal.getTimeInMillis() - startTime;
            try {
                Thread.sleep( SLEEP_LENGTH );
            }
            catch ( InterruptedException e ) {
                LOG.warn( "Thread interrupted while sleeping", e );
            }
        }
        while ( timePassed < timeout && instanceIdCopy.size() > 0 );

        return ( timePassed < timeout );
    }


    /**
     * @param stack Coordinated stack whose definition will be returned
     * @return      Definition string containing stack's user, module, commit and name
     */
    protected static String getLongName( ICoordinatedStack stack ) {

        StringBuilder sb = new StringBuilder();
        sb.append( stack.getUser().getUsername() )
                .append( "-" ).append( stack.getModule().getGroupId() )
                .append( "-" ).append( stack.getModule().getArtifactId() )
                .append( "-" ).append( stack.getModule().getVersion() )
                .append( "-" ).append( stack.getCommit().getId() )
                .append( "-" ).append( stack.getName() );

        return sb.toString();
    }


    /**
     * @param stack     <code>ICoordinatedStack</code> object containing the <code>cluster</code>
     * @param cluster   cluster whose name will be returned
     * @return          Concatenates hash code of <code>getLongName</code> of given stack with cluster's name,
     *                  resulting a unique name for each cluster
     */
    protected static String getInstanceName( ICoordinatedStack stack, ICoordinatedCluster cluster ) {
        StringBuilder sb = new StringBuilder();
        int stackHash = getLongName( stack ).hashCode();
        if( stackHash < 0 ) {
            stackHash += Integer.MAX_VALUE;
        }
        sb.append( stackHash ).append( "-" ).append( cluster.getName() );
        return sb.toString();
    }


    /**
     * @param stack <code>ICoordinatedStack</code> object the runners belong to
     * @return      Concatenates hash code of <code>getLongName</code> of given stack with '-runner' suffix,
     *              resulting a unique name for each stack
     */
    protected static String getRunnerName( ICoordinatedStack stack ) {
        StringBuilder sb = new StringBuilder();
        int stackHash = getLongName( stack ).hashCode();
        if( stackHash < 0 ) {
            stackHash += Integer.MAX_VALUE;
        }
        sb.append( stackHash ).append( "-runner" );
        return sb.toString();
    }
}
