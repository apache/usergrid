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
package org.apache.usergrid.tools;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerator;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.AggregateCounter;
import org.apache.usergrid.tools.bean.MetricLine;
import org.apache.usergrid.tools.bean.MetricQuery;
import org.apache.usergrid.tools.bean.MetricSort;
import org.apache.usergrid.utils.TimeUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.time.DateUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Ordering;
import org.apache.usergrid.persistence.index.query.CounterResolution;


/**
 * Tools class which dumps metrics for tracking Usergrid developer adoption and high-level application usage.
 * <p/>
 * Can be called thusly: mvn exec:java -Dexec.mainClass="org.apache.usergrid.tools.Command" -Dexec.args="Metrics -host
 * localhost -outputDir ./output"
 *
 * @author zznate
 */
public class Metrics extends ExportingToolBase {

    private List<OrganizationInfo> organizations;
    private ListMultimap<UUID, ApplicationInfo> orgApps = ArrayListMultimap.create();
    private ListMultimap<Long, UUID> totalScore = ArrayListMultimap.create();
    private Map<UUID, MetricLine> collector = new HashMap<UUID, MetricLine>();
    private int reportThreshold = 100;
    private long startDate;
    private long endDate;


    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        setVerbose( line );

        prepareBaseOutputFileName( line );

        parseDuration( line );

        applyOrgId( line );

        parseDateRange( line );

        outputDir = createOutputParentDir();

        logger.info( "Export directory: {}", outputDir.getAbsolutePath() );

        if ( orgId == null ) {
            organizations = managementService.getOrganizations( null, 20000 );
            for ( OrganizationInfo organization : organizations ) {
                logger.info( "Org Name: {} key: {}", organization.getName(), organization.getUuid() );
                //List<UserInfo> adminUsers = managementService.getAdminUsersForOrganization(orgId);
                applicationsFor( organization.getUuid() );
            }
        }
        else {
            OrganizationInfo orgInfo = managementService.getOrganizationByUuid( orgId );
            applicationsFor( orgInfo.getUuid() );
            organizations = new ArrayList<OrganizationInfo>();
            organizations.add( orgInfo );
        }

        Iterable<OrganizationInfo> workingOrgs = applyThreshold();

        printReport( MetricSort.APP_REQ_COUNT, workingOrgs );
    }


    @Override
    public Options createOptions() {
        Options options = super.createOptions();
        Option duration = OptionBuilder.hasArg().withDescription( "A duration signifying the previous time until now. "
                + "Supported forms: h,m,d eg. '30d' would be 30 days" ).create( "duration" );
        Option startDate =
                OptionBuilder.hasArg().withDescription( "The start date of the report" ).create( "startDate" );
        Option endDate = OptionBuilder.hasArg().withDescription( "The end date of the report" ).create( "endDate" );

        options.addOption( duration ).addOption( endDate ).addOption( startDate );

        return options;
    }


    /** 30 days in milliseconds by default */
    private void parseDuration( CommandLine line ) {
        String duration = line.getOptionValue( "duration" );
        if ( duration != null ) {
            startDate = TimeUtils.millisFromDuration( duration );
            endDate = System.currentTimeMillis();
        }
    }


    private void parseDateRange( CommandLine line ) throws Exception {
        if ( line.hasOption( "startDate" ) ) {
            startDate = DateUtils.parseDate( line.getOptionValue( "startDate" ), new String[] { "yyyyMMdd-HHmm" } )
                                 .getTime();
        }
        if ( line.hasOption( "endDate" ) ) {
            endDate =
                    DateUtils.parseDate( line.getOptionValue( "endDate" ), new String[] { "yyyyMMdd-HHmm" } ).getTime();
        }
    }


    private Iterable<OrganizationInfo> applyThreshold() throws Exception {
        Set<OrganizationInfo> orgs = new HashSet<OrganizationInfo>( reportThreshold );
        for ( Long l : Ordering.natural().greatestOf( totalScore.keys(), reportThreshold ) ) {
            List<UUID> apps = totalScore.get( l );
            for ( UUID appId : apps ) {
                orgs.add( managementService.getOrganizationForApplication( appId ) );
            }
        }
        return orgs;
    }


    private void printReport( MetricSort metricSort, Iterable<OrganizationInfo> workingOrgs ) throws Exception {
        JsonGenerator jg = getJsonGenerator( createOutputFile( "metrics", metricSort.name().toLowerCase() ) );
        jg.writeStartObject();
        jg.writeStringField( "report", metricSort.name() );
        jg.writeStringField( "date", new Date().toString() );
        jg.writeArrayFieldStart( "orgs" );
        for ( OrganizationInfo org : workingOrgs ) {
            jg.writeStartObject();
            jg.writeStringField( "org_id", org.getUuid().toString() );
            jg.writeStringField( "org_name", org.getName() );
            jg.writeArrayFieldStart( "admins" );
            for ( UserInfo userInfo : managementService.getAdminUsersForOrganization( org.getUuid() ) ) {
                jg.writeString( userInfo.getEmail() );
            }
            jg.writeEndArray();
            writeAppLines( jg, org.getUuid() );
            jg.writeEndObject();
        }
        jg.writeEndArray();
        jg.writeEndObject();
        jg.close();
    }


    private void writeAppLines( JsonGenerator jg, UUID orgId ) throws Exception {
        jg.writeArrayFieldStart( "apps" );
        for ( ApplicationInfo appInfo : orgApps.get( orgId ) ) {

            jg.writeStartObject();
            jg.writeStringField( "app_id", appInfo.getId().toString() );
            jg.writeStringField( "app_name", appInfo.getName() );
            jg.writeArrayFieldStart( "counts" );
            MetricLine line = collector.get( appInfo.getId() );
            if ( line != null ) {
                jg.writeStartObject();
                for ( AggregateCounter ag : line.getAggregateCounters() ) {
                    jg.writeStringField( new Date( ag.getTimestamp() ).toString(), Long.toString( ag.getValue() ) );
                }
                jg.writeEndObject();
            }
            jg.writeEndArray();
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }


    private void applicationsFor( UUID orgId ) throws Exception {
        BiMap<UUID, String> applications = managementService.getApplicationsForOrganization( orgId );

        for ( UUID uuid : applications.keySet() ) {
            logger.info( "Checking app: {}", applications.get( uuid ) );

            orgApps.put( orgId, new ApplicationInfo( uuid, applications.get( uuid ) ) );

            collect( MetricQuery.getInstance( uuid, MetricSort.APP_REQ_COUNT ).resolution( CounterResolution.DAY )
                                .startDate( startDate ).endDate( endDate ).execute( emf.getEntityManager( uuid ) ) );
        }
    }


    private void collect( MetricLine metricLine ) {
        for ( AggregateCounter a : metricLine.getAggregateCounters() ) {
            logger.info( "col: {} val: {}", new Date( a.getTimestamp() ), a.getValue() );
        }
        totalScore.put( metricLine.getCount(), metricLine.getAppId() );
        collector.put( metricLine.getAppId(), metricLine );
    }
    // line format: {reportQuery: application.requests, date: date, startDate : startDate, endDate: endDate, orgs : [
    // {orgId: guid, orgName: name, apps [{appId: guid, appName: name, dates: [{"[human date from ts]" : "[value]"},{...
}
