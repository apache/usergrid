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
package org.apache.usergrid.rest.applications.kvm;

import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.map.MapKeyResults;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.applications.ServiceResource;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.*;

@Component("org.apache.usergrid.rest.applications.kvm.KvmResource")
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
})
public class KvmResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( KvmResource.class );


    private final int PAGE_SIZE_MAX = 1000;
    private final int PAGE_SIZE_DEFAULT = 10;
    private final int BATCH_SIZE_MAX = 10;

    ServiceManager services = null;

    @Autowired
    private Injector injector;

    public KvmResource(){
        if(logger.isTraceEnabled()) {
            logger.trace("kvm.KvmResource");
        }
    }

    @Override
    public void setParent( AbstractContextResource parent ) {
        super.setParent( parent );
        if ( parent instanceof ServiceResource ) {
            services = ( ( ServiceResource ) parent ).services;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse createMap( @Context UriInfo ui,
                                  Map<String, Object> json ) {

        if( json.keySet().size() < 1 ){
            throw new IllegalArgumentException("New map must be specified");
        }
        if( json.keySet().size() > 1 ){
            throw new IllegalArgumentException("Only 1 map can be created at a time.");
        }

        final String mapName = json.keySet().iterator().next();
        final Object meta = json.get(mapName);
        if( !(meta instanceof Map) ){
            throw new IllegalArgumentException("New map details must be provided as a JSON object.");
        }

        if(StringUtils.isEmpty(mapName)){
            throw new IllegalArgumentException("Property mapName is required.");
        }

        final String appMap = services.getApplicationId().toString();
        final MapScope mapScope = new MapScopeImpl(services.getApplication().asId(), appMap);
        final MapManagerFactory mmf = injector.getInstance(MapManagerFactory.class);
        final MapManager mapManager = mmf.createMapManager(mapScope);

        final Map<String, Object> mapDetails = new HashMap<String, Object>(){{

            put("active", true);
            put("modified", System.currentTimeMillis() );
            put("details", meta);

        }};

        mapManager.putString(mapName, JsonUtils.mapToJsonString(mapDetails));

        // This used for response format
        final Map<String, Object> map = new HashMap<>(2);
        map.put(mapName, mapDetails);


        ApiResponse response = createApiResponse();
        response.setAction( "post" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        response.setProperty("maps", Collections.singletonList(map));

        return response;

    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse getMaps( @Context UriInfo ui,
                                @QueryParam("limit") int limit,
                                @QueryParam("cursor") String cursor) {

        limit = validateLimit(limit);

        final String appMap = services.getApplicationId().toString();

        final MapScope mapScope = new MapScopeImpl(services.getApplication().asId(), appMap);
        final MapManagerFactory mmf = injector.getInstance(MapManagerFactory.class);
        final MapManager mapManager = mmf.createMapManager(mapScope);

        MapKeyResults mapKeyResults = mapManager.getKeys(cursor, limit );

        List<Map<String, Object>> results = new ArrayList<>();

        Observable.from(mapKeyResults.getKeys()).flatMap(key -> {
            return Observable.just(new HashMap<String, Object>() {{
                put(key, JsonUtils.parse(mapManager.getString(key)));
            }});

        }).doOnNext(entry -> results.add(entry)).toBlocking().lastOrDefault(null);


        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        response.setProperty("maps", results);

        if( StringUtils.isNotEmpty(mapKeyResults.getCursor())){
            response.setProperty("cursor", mapKeyResults.getCursor());
        }

        return response;

    }

    @GET
    @Path("{mapName}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse getSingleMap( @Context UriInfo ui,
                                @QueryParam("limit") int limit,
                                @QueryParam("cursor") String cursor,
                                @PathParam("mapName") String mapName ) {

        limit = validateLimit(limit);

        final String appMap = services.getApplicationId().toString();

        final MapScope mapScope = new MapScopeImpl(services.getApplication().asId(), appMap);
        final MapManagerFactory mmf = injector.getInstance(MapManagerFactory.class);
        final MapManager mapManager = mmf.createMapManager(mapScope);


        Map<String, Object> map = new HashMap<>();
        map.put(mapName, JsonUtils.parse(mapManager.getString(mapName)));


        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        response.setProperty("maps", Collections.singletonList(map));

        return response;

    }


    @POST
    @Path("{mapName}/entries")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse createEntries( @Context UriInfo ui,
                                      List<Map<String,Object>> json,
                                      @PathParam("mapName") String mapName ) {

        if ( json.size() > BATCH_SIZE_MAX ){
            throw new IllegalArgumentException("Batch size "+json.size()+" is more than max size of "+BATCH_SIZE_MAX);
        }

        if(StringUtils.isEmpty(mapName)){
            throw new IllegalArgumentException("Property mapName is required.");
        }

        final MapScope mapScope = new MapScopeImpl(services.getApplication().asId(), mapName);
        final MapManagerFactory mmf = injector.getInstance(MapManagerFactory.class);
        final MapManager mapManager = mmf.createMapManager(mapScope);

        //TODO maybe RX flatmap this or parallel stream
        json.forEach( item -> {

            String key = item.keySet().iterator().next();
            Object value = item.get(key);

            mapManager.putString(key, value.toString());


        });


        ApiResponse response = createApiResponse();
        response.setAction( "post" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        response.setProperty("mapName", mapName);
        response.setProperty("entries", json);

        return response;

    }

    @GET
    @Path("{mapName}/keys")
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse getKeys( @Context UriInfo ui,
                                @QueryParam("limit") int limit,
                                @QueryParam("cursor") String cursor,
                                @PathParam("mapName") String mapName ) {


        if(StringUtils.isEmpty(mapName)){
            throw new IllegalArgumentException("Property mapName is required.");
        }

        limit = validateLimit(limit);

        final MapScope mapScope = new MapScopeImpl(services.getApplication().asId(), mapName);
        final MapManagerFactory mmf = injector.getInstance(MapManagerFactory.class);
        final MapManager mapManager = mmf.createMapManager(mapScope);

        MapKeyResults mapKeyResults = mapManager.getKeys(cursor, limit );


        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        response.setProperty("keys", mapKeyResults.getKeys());

        if( StringUtils.isNotEmpty(mapKeyResults.getCursor())){
            response.setProperty("cursor", mapKeyResults.getCursor());
        }

        return response;

    }

    @GET
    @Path("{mapName}/entries")
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse getEntries( @Context UriInfo ui,
                                   @QueryParam("limit") int limit,
                                   @QueryParam("cursor") String cursor,
                                   @PathParam("mapName") String mapName ) {

        if(StringUtils.isEmpty(mapName)){
            throw new IllegalArgumentException("Property mapName is required.");
        }

        limit = validateLimit(limit);

        final MapScope mapScope = new MapScopeImpl(services.getApplication().asId(), mapName);
        final MapManagerFactory mmf = injector.getInstance(MapManagerFactory.class);
        final MapManager mapManager = mmf.createMapManager(mapScope);

        MapKeyResults mapKeyResults = mapManager.getKeys(cursor, limit );

        List<Map<String, String>> results = new ArrayList<>();
        Observable.from(mapKeyResults.getKeys()).flatMap(key ->{
            return Observable.just(new HashMap<String, String>(){{put(key, mapManager.getString(key));}});

        }).doOnNext(entry -> results.add(entry)).toBlocking().lastOrDefault(null);


        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        response.setProperty("entries", results);

        if( StringUtils.isNotEmpty(mapKeyResults.getCursor())){
            response.setProperty("cursor", mapKeyResults.getCursor());
        }

        return response;

    }

    @GET
    @Path("{mapName}/entries/{keyName}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse getEntry( @Context UriInfo ui,
                                   @PathParam("mapName") String mapName,
                                   @PathParam("keyName") String keyName ) {


        final MapScope mapScope = new MapScopeImpl(services.getApplication().asId(), mapName);
        final MapManagerFactory mmf = injector.getInstance(MapManagerFactory.class);
        final MapManager mapManager = mmf.createMapManager(mapScope);


        List<Map<String, String>> results = new ArrayList<>();
        results.add(new HashMap<String, String>(){{put(keyName, mapManager.getString(keyName));}});


        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        response.setProperty("entries", results);

        return response;

    }


    private int validateLimit(final int limit){

        return limit > 0 && limit < PAGE_SIZE_MAX ? limit : PAGE_SIZE_DEFAULT;

    }


}
