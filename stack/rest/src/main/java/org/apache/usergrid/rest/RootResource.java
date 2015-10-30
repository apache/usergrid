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
package org.apache.usergrid.rest;


import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.collect.BiMap;
import com.google.inject.Injector;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.stats.Snapshot;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.rest.applications.ApplicationResource;
import org.apache.usergrid.rest.exceptions.NoOpException;
import org.apache.usergrid.rest.organizations.OrganizationResource;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.apache.usergrid.system.UsergridSystemMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;


/** @author ed@anuff.com */
@Path("/")
@Component
@Scope("singleton")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class RootResource extends AbstractContextResource implements MetricProcessor<RootResource.MetricContext> {

    static final class MetricContext {
        final boolean showFullSamples;
        final ObjectNode objectNode;


        MetricContext( ObjectNode objectNode, boolean showFullSamples ) {
            this.objectNode = objectNode;
            this.showFullSamples = showFullSamples;
        }
    }


    private static final Logger logger = LoggerFactory.getLogger( RootResource.class );

    long started = System.currentTimeMillis();

    @Autowired
    private UsergridSystemMonitor usergridSystemMonitor;

    @Autowired
    private Injector injector;


    public RootResource() {
    }


    @RequireSystemAccess
    @GET
    @Path("applications")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getAllApplications(
        @Context UriInfo ui,
        @QueryParam("deleted") @DefaultValue("false") Boolean deleted,
        @QueryParam("callback") @DefaultValue("callback") String callback ) throws URISyntaxException {

        logger.info( "RootResource.getData" );

        ApiResponse response = createApiResponse();
        response.setAction( "get applications" );

        Map<String, UUID> applications = null;
        try {
            if ( deleted ) {
                applications = emf.getDeletedApplications();
            } else {
                applications = emf.getApplications();
            }
            response.setSuccess();
            response.setApplications( applications );
        }
        catch ( Exception e ) {
            logger.info( "Unable to retrieve applications", e );
            response.setError( "Unable to retrieve applications" );
        }

        return response;
    }


    @RequireSystemAccess
    @GET
    @Path("apps")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getAllApplications2( @Context UriInfo ui,
                                                @QueryParam("callback") @DefaultValue("callback") String callback )
            throws URISyntaxException {
        return getAllApplications( ui, false, callback );
    }


    @GET
    public Response getRoot( @Context UriInfo ui ) throws URISyntaxException {

        String redirect_root = properties.getRedirectRoot();
        if ( StringUtils.isNotBlank( redirect_root ) ) {
            ResponseBuilder response = Response.temporaryRedirect( new URI( redirect_root ) );
            return response.build();
        }
        else {
            ResponseBuilder response = Response.temporaryRedirect( new URI( "/status" ) );
            return response.build();
        }
    }


    /**
     * Return status of this Usergrid instance in JSON format.
     *
     * By Default this end-point will ignore errors but if you call it with ignore_status=false
     * then it will return HTTP 500 if either the Entity store or the Index for the management
     * application are in a bad state.
     *
     * @param ignoreError Ignore any errors and return status no matter what.
     */
    @GET
    @Path("status")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getStatus(
            @QueryParam("ignore_error") @DefaultValue("true") Boolean ignoreError,
            @QueryParam("callback") @DefaultValue("callback") String callback ) {

        ApiResponse response = createApiResponse();

        AsyncEventService eventService = injector.getInstance(AsyncEventService.class);


        if ( !ignoreError ) {

            if ( !emf.getEntityStoreHealth().equals( Health.GREEN )) {
                throw new RuntimeException("Error connecting to datastore");
            }


            if ( emf.getIndexHealth().equals( Health.RED) ) {
                throw new RuntimeException("Management app index is status RED");
            }
        }

        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put( "started", started );
        node.put( "uptime", System.currentTimeMillis() - started );
        node.put( "version", usergridSystemMonitor.getBuildNumber());

        // Hector status, for backwards compatibility
        node.put("cassandraAvailable", usergridSystemMonitor.getIsCassandraAlive());

        // Core Persistence Collections module status
        node.put( "cassandraStatus", emf.getEntityStoreHealth().toString() );

        // Core Persistence Query Index module status for Management App Index
        node.put( "managementAppIndexStatus", emf.getIndexHealth().toString() );
        node.put( "queueDepth", eventService.getQueueDepth() );


        dumpMetrics(node);
        response.setProperty( "status", node );
        return response;
    }


    @GET
    @Path("lb-status")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getLbStatus() {
        ResponseBuilder response;
        if ( usergridSystemMonitor.getIsCassandraAlive() ) {
            response = Response.noContent().status( Response.Status.OK );
        }
        else {
            response = Response.noContent().status( Response.Status.SERVICE_UNAVAILABLE );
        }
        return response.build();
    }


    private void dumpMetrics( ObjectNode node ) {
        MetricsRegistry registry = Metrics.defaultRegistry();

        for ( Map.Entry<String, SortedMap<MetricName, Metric>> entry : registry.groupedMetrics().entrySet() ) {

            ObjectNode meterNode = JsonNodeFactory.instance.objectNode();

            for ( Map.Entry<MetricName, Metric> subEntry : entry.getValue().entrySet() ) {
                ObjectNode metricNode = JsonNodeFactory.instance.objectNode();

                try {
                    subEntry.getValue().processWith( this, subEntry.getKey(), new MetricContext( metricNode, true ) );
                }
                catch ( Exception e ) {
                    logger.warn( "Error writing out {}", subEntry.getKey(), e );
                }
                meterNode.put( subEntry.getKey().getName(), metricNode );
            }
            node.put( entry.getKey(), meterNode );
        }
    }


    @Path("{applicationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
    public ApplicationResource getApplicationById( @PathParam("applicationId") String applicationIdStr )
            throws Exception {

        if ( "options".equalsIgnoreCase( request.getMethod() ) ) {
            throw new NoOpException();
        }

        UUID applicationId = UUID.fromString( applicationIdStr );
        if ( applicationId == null ) {
            return null;
        }

        return appResourceFor( applicationId );
    }


    private ApplicationResource appResourceFor( UUID applicationId ) throws Exception {
        if ( applicationId.equals( emf.getManagementAppId() ) ) {
            throw new UnauthorizedException();
        }

        return getSubResource( ApplicationResource.class ).init( applicationId );
    }


    @Path("applications/"+APPLICATION_ID_PATH)
    public ApplicationResource getApplicationById2( @PathParam("applicationId") String applicationId )
            throws Exception {
        return getApplicationById( applicationId );
    }


    @Path("apps/"+APPLICATION_ID_PATH)
    public ApplicationResource getApplicationById3( @PathParam("applicationId") String applicationId )
            throws Exception {
        return getApplicationById( applicationId );
    }

    public static final String APPLICATION_ID_PATH = "{applicationId: " + Identifier.UUID_REX + "}";
    public static final String ORGANIZATION_ID_PATH = "{organizationId: " + Identifier.UUID_REX + "}";
    public static final String USER_ID_PATH = "{userId: " + Identifier.UUID_REX + "}";
    public static final String ENTITY_ID_PATH = "{entityId: " + Identifier.UUID_REX + "}";
    public static final String EMAIL_PATH = "{email: " + Identifier.EMAIL_REX + "}";

    @Path(ORGANIZATION_ID_PATH+"/"+APPLICATION_ID_PATH)
    public ApplicationResource getApplicationByUuids( @PathParam("organizationId") String organizationIdStr,
                                                      @PathParam("applicationId") String applicationIdStr )

            throws Exception {

        UUID applicationId = UUID.fromString( applicationIdStr );
        UUID organizationId = UUID.fromString( organizationIdStr );
        if ( applicationId == null || organizationId == null ) {
            return null;
        }
        BiMap<UUID, String> apps = management.getApplicationsForOrganization( organizationId );
        if ( apps.get( applicationId ) == null ) {
            return null;
        }
        return appResourceFor( applicationId );
    }


    private OrganizationResource orgResourceFor( String organizationName ) throws Exception {

        return getSubResource( OrganizationResource.class ).init( organizationName );
    }


    @Path("{organizationName}")
    public OrganizationResource getOrganizationByName( @PathParam("organizationName") String organizationName )
            throws Exception {

        if ( "options".equalsIgnoreCase( request.getMethod() ) ) {
            throw new NoOpException();
        }

        return orgResourceFor( organizationName );
    }


    @Path("organizations/{organizationName}")
    public OrganizationResource getOrganizationByName2( @PathParam("organizationName") String organizationName )
            throws Exception {
        return getOrganizationByName( organizationName );
    }


    @Path("orgs/{organizationName}")
    public OrganizationResource getOrganizationByName3( @PathParam("organizationName") String organizationName )
            throws Exception {
        logger.debug("getOrganizationByName3");
        return getOrganizationByName( organizationName );
    }


    @Path("o/{organizationName}")
    public OrganizationResource getOrganizationByName4( @PathParam("organizationName") String organizationName )
            throws Exception {
        return getOrganizationByName( organizationName );
    }


    @Override
    public void processHistogram( MetricName name, Histogram histogram, MetricContext context ) throws Exception {
        final ObjectNode node = context.objectNode;
        node.put( "type", "histogram" );
        node.put( "count", histogram.count() );
        writeSummarizable( histogram, node );
        writeSampling( histogram, node );
    }


    @Override
    public void processCounter( MetricName name, Counter counter, MetricContext context ) throws Exception {
        final ObjectNode node = context.objectNode;
        node.put( "type", "counter" );
        node.put( "count", counter.count() );
    }


    @Override
    public void processGauge( MetricName name, Gauge<?> gauge, MetricContext context ) throws Exception {
        final ObjectNode node = context.objectNode;
        node.put( "type", "gauge" );
        node.put( "vale", "[disabled]" );
    }


    @Override
    public void processMeter( MetricName name, Metered meter, MetricContext context ) throws Exception {
        final ObjectNode node = context.objectNode;
        node.put( "type", "meter" );
        node.put( "event_type", meter.eventType() );
        writeMeteredFields( meter, node );
    }


    @Override
    public void processTimer( MetricName name, Timer timer, MetricContext context ) throws Exception {
        final ObjectNode node = context.objectNode;

        node.put( "type", "timer" );
        // json.writeFieldName("duration");
        node.put( "unit", timer.durationUnit().toString().toLowerCase() );
        ObjectNode durationNode = JsonNodeFactory.instance.objectNode();
        writeSummarizable( timer, durationNode );
        writeSampling( timer, durationNode );
        node.put( "duration", durationNode );
        writeMeteredFields( timer, node );
    }


    private static void writeSummarizable( Summarizable metric, ObjectNode mNode ) throws IOException {
        mNode.put( "min", metric.min() );
        mNode.put( "max", metric.max() );
        mNode.put( "mean", metric.mean() );
        mNode.put( "std_dev", metric.stdDev() );
    }


    private static void writeSampling( Sampling metric, ObjectNode mNode ) throws IOException {

        final Snapshot snapshot = metric.getSnapshot();
        mNode.put( "median", snapshot.getMedian() );
        mNode.put( "p75", snapshot.get75thPercentile() );
        mNode.put( "p95", snapshot.get95thPercentile() );
        mNode.put( "p98", snapshot.get98thPercentile() );
        mNode.put( "p99", snapshot.get99thPercentile() );
        mNode.put( "p999", snapshot.get999thPercentile() );
    }


    private static void writeMeteredFields( Metered metered, ObjectNode node ) throws IOException {
        ObjectNode mNode = JsonNodeFactory.instance.objectNode();
        mNode.put( "unit", metered.rateUnit().toString().toLowerCase() );
        mNode.put( "count", metered.count() );
        mNode.put( "mean", metered.meanRate() );
        mNode.put( "m1", metered.oneMinuteRate() );
        mNode.put( "m5", metered.fiveMinuteRate() );
        mNode.put( "m15", metered.fifteenMinuteRate() );
        node.put( "rate", mNode );
    }
}
