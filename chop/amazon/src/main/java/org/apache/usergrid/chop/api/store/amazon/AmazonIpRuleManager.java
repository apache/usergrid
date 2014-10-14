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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.usergrid.chop.stack.BasicIpRule;
import org.apache.usergrid.chop.spi.IpRuleManager;
import org.apache.usergrid.chop.stack.BasicIpRuleSet;
import org.apache.usergrid.chop.stack.IpRule;
import org.apache.usergrid.chop.stack.IpRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.google.inject.Inject;


/** TODO check outbound rules */
public class AmazonIpRuleManager implements IpRuleManager {

    private static final Logger LOG = LoggerFactory.getLogger( AmazonIpRuleManager.class );

    private AmazonEC2Client client;


    @Inject
    public AmazonIpRuleManager( AmazonFig amazonFig ) {
        client = AmazonUtils.getEC2Client( amazonFig.getAwsAccessKey(), amazonFig.getAwsSecretKey() );
    }


    /**
     * Note that you have to set the data center before any other operation,
     * if you are to use a different data center than Amazon's default
     */
    @Override
    public void setDataCenter( final String dataCenter ) {
        client.setEndpoint( AmazonUtils.getEndpoint( dataCenter ) );
    }


    @Override
    public void applyIpRuleSet( final IpRuleSet ruleSet ) {
        if( exists( ruleSet.getName() ) ) {
            Collection<IpRule> inbound = getRules( ruleSet.getName(), true );
            Collection<IpRule> outbound = getRules( ruleSet.getName(), false );
            deleteRules( ruleSet.getName(), inbound );
            deleteRules( ruleSet.getName(), outbound );
        }
        else {
            createRuleSet( ruleSet.getName() );
        }

        for( IpRule rule: ruleSet.getInboundRules() ) {
            addRules( ruleSet.getName(), rule.getIpRanges(), rule.getIpProtocol(), rule.getFromPort(),
                    rule.getToPort() );
        }
        for( IpRule rule: ruleSet.getOutboundRules() ) {
            addRules( ruleSet.getName(), rule.getIpRanges(), rule.getIpProtocol(), rule.getFromPort(),
                                rule.getToPort() );
        }
    }


    @Override
    public IpRuleSet getIpRuleSet( final String name ) {
        Collection<IpRule> inbound = getRules( name, true );
        Collection<IpRule> outbound = getRules( name, false );

        BasicIpRuleSet ruleSet = new BasicIpRuleSet();
        ruleSet.setName( name );
        for( IpRule rule: inbound ) {
            ruleSet.addInboundRule( rule );
        }
        for( IpRule rule: outbound ) {
            ruleSet.addOutboundRule( rule );
        }

        return ruleSet;
    }


    @Override
    public boolean createRuleSet( final String name ) {
        try {
            CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();

            request = request.withGroupName( name ).withDescription( "Judo Chop Security Group" );
            CreateSecurityGroupResult result = client.createSecurityGroup( request );
            return ( result != null && result.getGroupId() != null && ! result.getGroupId().isEmpty() );
        }
        catch ( AmazonServiceException e ) {
            LOG.warn( "Error while trying to create security group", e );
            return false;
        }
    }


    @Override
    public boolean deleteRuleSet( final String name ) {
        try {
            DeleteSecurityGroupRequest request = new DeleteSecurityGroupRequest().withGroupName( name );
            client.deleteSecurityGroup( request );
            return true;
        }
        catch ( AmazonServiceException e ) {
            LOG.warn( "Error while trying to delete security group", e );
            return false;
        }
    }


    @Override
    public Collection<String> listRuleSets() {
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult result = null;
        try {
            result = client.describeSecurityGroups( request );
        }
        catch ( Exception e ) {
            LOG.warn( "Error while getting security groups", e );
            return new LinkedList<String>();
        }
        Collection<String> groups = new ArrayList<String>();
        for( SecurityGroup group : result.getSecurityGroups() ) {
            groups.add( group.getGroupName() );
        }
        return groups;
    }


    @Override
    public boolean exists( final String name ) {
        Collection<String> groups = listRuleSets();
        for( String g : groups ) {
            if ( g.equals( name ) ) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Collection<IpRule> getRules( final String name, final boolean inbound ) {
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest().withGroupNames( name );
        DescribeSecurityGroupsResult result = client.describeSecurityGroups( request );

        if( result.getSecurityGroups().size() != 1 ) {
            return null;
        }

        Collection<IpRule> ipRules = new ArrayList<IpRule>();
        List<IpPermission> permissions;

        if( inbound ) {
            permissions = result.getSecurityGroups().get( 0 ).getIpPermissions();
        }
        else {
            permissions = result.getSecurityGroups().get( 0 ).getIpPermissionsEgress();
        }

        for( IpPermission permission : permissions ) {
            ipRules.add( toIpRule( permission ) );
        }

        return ipRules;
    }


    @Override
    public void deleteRules( final String name, final IpRule... ipRules ) {
        if( ipRules.length == 0 ) {
            return;
        }
        Collection<IpRule> rules = new ArrayList<IpRule>( ipRules.length );
        for( IpRule rule: ipRules ) {
            rules.add( rule );
        }
        deleteRules( name, rules );
    }


    @Override
    public void deleteRules( final String name, final Collection<IpRule> ipRules ) {
        if( ipRules == null || ipRules.size() == 0 ) {
            return;
        }
        Collection<IpPermission> permissions = new ArrayList<IpPermission>( ipRules.size() );
        for( IpRule rule : ipRules ) {
            permissions.add( toIpPermission( rule ) );
        }

        RevokeSecurityGroupIngressRequest request = new RevokeSecurityGroupIngressRequest();
        request = request.withGroupName( name ).withIpPermissions( permissions );
        client.revokeSecurityGroupIngress( request );
    }


    @Override
    public void deleteRules( final String name, final Collection<String> ipRanges, final String protocol,
                             final int port ) {
        IpPermission permission = new IpPermission();
        permission = permission.withIpProtocol( protocol )
                               .withFromPort( port )
                               .withToPort( port )
                               .withIpRanges( ipRanges );

        RevokeSecurityGroupIngressRequest request = new RevokeSecurityGroupIngressRequest();
        request = request.withGroupName( name ).withIpPermissions( permission );

        client.revokeSecurityGroupIngress( request );
    }


    @Override
    public void addRules( final String name, final Collection<String> ipRanges, final String protocol,
                          final int port ) {
        addRules( name, ipRanges, protocol, port, port );
    }


    @Override
    public void addRules( final String name, final Collection<String> ipRanges, final String protocol,
                          final int fromPort, final int toPort ) {

        IpPermission ipPermission = new IpPermission();

        ipPermission.withIpRanges( ipRanges )
                    .withIpProtocol( protocol )
                    .withFromPort( fromPort )
                    .withToPort( toPort );

        try {
            AuthorizeSecurityGroupIngressRequest request = new AuthorizeSecurityGroupIngressRequest();
            request = request.withGroupName( name ).withIpPermissions( ipPermission );
            client.authorizeSecurityGroupIngress( request );
        }
        catch ( Exception e ) {
            LOG.error( "Error whilt adding rule to security group: {}", name, e );
        }
    }


    protected static IpRule toIpRule( IpPermission permission ) {
        BasicIpRule rule = new BasicIpRule();
        rule.setFromPort( permission.getFromPort() );
        rule.setToPort( permission.getToPort() );
        rule.setIpProtocol( permission.getIpProtocol() );
        rule.setIpRanges( permission.getIpRanges() );

        return rule;
    }


    protected static IpPermission toIpPermission( IpRule rule ) {
        IpPermission permission = new IpPermission();
        permission.setIpProtocol( rule.getIpProtocol() );
        permission.setToPort( rule.getToPort() );
        permission.setFromPort( rule.getFromPort() );
        permission.setIpRanges( rule.getIpRanges() );

        return permission;
    }

}
