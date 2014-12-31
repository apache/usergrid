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
package org.apache.usergrid.rest.test.resource;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.usergrid.persistence.AggregateCounterSet;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.rest.ServerEnvironmentProperties;
import org.apache.usergrid.security.oauth.ClientCredentialsInfo;
import org.apache.usergrid.services.ServiceRequest;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.utils.InflectionUtils;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

import static org.apache.usergrid.utils.InflectionUtils.pluralize;
import org.apache.usergrid.rest.test.resource.app.model.Entity;

//TODO: code = node.getHTTPResponseCode(); We need a way to see the httpResponseCode
//TODO: Move this into the testing framework
@JsonPropertyOrder( {
        "action", "application", "params", "path", "query", "uri", "status", "error", "applications", "entity",
        "entities", "list", "data", "next", "timestamp", "duration"
} )
@XmlRootElement
public class Response {

    private ServiceRequest esp;
//TODO: investigate that errors are all properly set/ http responsecode, error description, The error Code thrown form the stack.
    private String error;
    private String errorDescription;
    private String errorUri;
    private String exception; //illegal_argument
    private String callback;

    private String path;
    private String uri;
    private String status;
    private long timestamp;
    private String organization;
    private String applicationName;
    private UUID application;
    private List<Entity> entities;
    private UUID next;
    private String cursor;
    private Integer count;
    private String action;
    private List<Object> list;
    private Object data;
    private Map<String, UUID> applications;
    private Map<String, Object> metadata;
    private Map<String, List<String>> params;
    private List<AggregateCounterSet> counters;
    private ClientCredentialsInfo credentials;

    protected Map<String, Object> properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );

    @Autowired
    protected ServerEnvironmentProperties serverEnvironmentProperties;


    public Response() {
        timestamp = System.currentTimeMillis();
    }


    public Response( ServerEnvironmentProperties properties ) {
        this.serverEnvironmentProperties = properties;
        timestamp = System.currentTimeMillis();
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public String getCallback() {
        return callback;
    }


    public void setCallback( String callback ) {
        this.callback = callback;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public String getError() {
        return error;
    }


    @JsonProperty("error")
    public void setError( String code ) {
        error = code;
    }


    public static String exceptionToErrorCode( Throwable e ) {
        if ( e == null ) {
            return "service_error";
        }
        String s = ClassUtils.getShortClassName( e.getClass() );
        s = StringUtils.removeEnd( s, "Exception" );
        s = InflectionUtils.underscore( s ).toLowerCase();
        return s;
    }


    public Response withError( String code ) {
        return withError( code, null, null );
    }


    public void setError( Throwable e ) {
        setError( null, null, e );
    }


    public Response withError( Throwable e ) {
        return withError( null, null, e );
    }


    public void setError( String description, Throwable e ) {
        setError( null, description, e );
    }


    public Response withError( String description, Throwable e ) {
        return withError( null, description, e );
    }



    public void setError( String code, String description, Throwable e ) {
        if ( code == null ) {
            code = exceptionToErrorCode( e );
        }
        error = code;
        errorDescription = description;
        if ( e != null ) {
            if ( description == null ) {
                errorDescription = e.getMessage();
            }
            exception = e.getClass().getName();
        }
    }


    public Response withError( String code, String description, Throwable e ) {
        setError( code, description, e );
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    @JsonProperty( "error_description" )
    public String getErrorDescription() {
        return errorDescription;
    }


    @JsonProperty( "error_description" )
    public void setErrorDescription( String errorDescription ) {
        this.errorDescription = errorDescription;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    @JsonProperty( "error_uri" )
    public String getErrorUri() {
        return errorUri;
    }


    @JsonProperty( "error_uri" )
    public void setErrorUri( String errorUri ) {
        this.errorUri = errorUri;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public String getException() {
        return exception;
    }


    public void setException( String exception ) {
        this.exception = exception;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public String getPath() {
        return path;
    }


    public void setPath( String path ) {
        if ( path == null ) {
            this.path = null;
            uri = null;
        }
        this.path = path;
        //uri = createPath( path );
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        //not entirely sure this works
//        if( uri == null){
//            uri = createPath( getPath() );
//        }
        this.uri = uri;
    }


    public void setServiceRequest( ServiceRequest p ) {
        esp = p;
        if ( p != null ) {
            path = p.getPath();
            uri = createPath( path );
        }
    }


    public Response withServiceRequest( ServiceRequest p ) {
        setServiceRequest( p );
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public String getStatus() {
        return status;
    }


    public void setSuccess() {
        status = "ok";
    }


    public Response withSuccess() {
        status = "ok";
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public long getDuration() {
        return System.currentTimeMillis() - timestamp;
    }


    public void setTimestamp( long timestamp ) {
        this.timestamp = timestamp;
    }


    public Response withTimestamp( long timestamp ) {
        this.timestamp = timestamp;
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public long getTimestamp() {
        return timestamp;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public String getAction() {
        return action;
    }


    public void setAction( String action ) {
        this.action = action;
    }


    public Response withAction( String action ) {
        this.action = action;
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public UUID getApplication() {
        return application;
    }

    @JsonSerialize( include = Inclusion.NON_NULL )
    public String applicationName() {
        return applicationName;
    }


    /** @return the orgId */

    @JsonSerialize( include = Inclusion.NON_NULL )
    public String getOrganization() {
        return organization;
    }



    public void setApplication( Application app) {
        this.application = app.getUuid();
    }

    /** Set the application and organization information */
//    public void setApplication( Application app ) {
//        this.organization = app.getOrganizationName();
//        this.applicationName = app.getApplicationName();
//        this.application = app.getUuid();
//
//        if ( esp != null ) {
//            uri = createPath( esp.toString() );
//        }
//    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    @XmlAnyElement
    public List<Entity> getEntities() {
        return entities;
    }


    public void setEntities( List<Entity> entities ) {
        if ( entities != null ) {
            this.entities = entities;
        }
        else {
            this.entities = new ArrayList<Entity>();
        }
    }


    public Response withEntities( List<Entity> entities ) {
        setEntities( entities );
        return this;
    }


    public void setResults( ServiceResults results ) {
        if ( results != null ) {
            setPath( results.getPath() );
            //entities = results.getEntities();
            next = results.getNextResult();
            cursor = results.getCursor();
            counters = results.getCounters();
        }
        else {
            entities = new ArrayList<Entity>();
        }
    }


    public Response withResults( ServiceResults results ) {
        setResults( results );
        return this;
    }


    public Response withResultsCount( ServiceResults results ) {
        setResults( results );
        if ( results != null ) {
            count = results.size();
        }
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public UUID getNext() {
        return next;
    }


    public void setNext( UUID next ) {
        this.next = next;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public String getCursor() {
        return cursor;
    }


    public void setCursor( String cursor ) {
        this.cursor = cursor;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public Integer getCount() {
        return count;
    }


    public void setCount( Integer count ) {
        this.count = count;
    }


    public Response withEntity( Entity entity ) {
        entities = new ArrayList<Entity>();
        entities.add( entity );
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public List<Object> getList() {
        return list;
    }


    public void setList( List<Object> list ) {
        if ( list != null ) {
            this.list = list;
        }
        else {
            this.list = new ArrayList<Object>();
        }
    }


    public Response withList( List<Object> list ) {
        setList( list );
        return this;
    }


    public Response withListCount( List<Object> list ) {
        setList( list );
        if ( !list.isEmpty() ) {
            this.count = list.size();
        }
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public Object getData() {
        return data;
    }


    public void setData( Object data ) {
        if ( data != null ) {
            this.data = data;
        }
        else {
            this.data = new LinkedHashMap<String, Object>();
        }
    }


    public Response withData( Object data ) {
        setData( data );
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public List<AggregateCounterSet> getCounters() {
        return counters;
    }


    public void setCounters( List<AggregateCounterSet> counters ) {
        this.counters = counters;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public Map<String, UUID> getApplications() {
        return applications;
    }


    public void setApplications( Map<String, UUID> applications ) {
        this.applications = applications;
    }


    public Response withApplications( Map<String, UUID> applications ) {
        this.applications = applications;
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public ClientCredentialsInfo getCredentials() {
        return credentials;
    }


    public void setCredentials( ClientCredentialsInfo credentials ) {
        this.credentials = credentials;
    }


    public Response withCredentials( ClientCredentialsInfo credentials ) {
        this.credentials = credentials;
        return this;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public Map<String, List<String>> getParams() {
        return params;
    }


    public void setParams( Map<String, List<String>> params ) {
        Map<String, List<String>> q = new LinkedHashMap<String, List<String>>();
        for ( String k : params.keySet() ) {
            List<String> v = params.get( k );
            if ( v != null ) {
                q.put( k, new ArrayList<String>( v ) );
            }
        }
        this.params = q;
    }


    @JsonSerialize( include = Inclusion.NON_NULL )
    public Map<String, Object> getMetadata() {
        return metadata;
    }


    public void setMetadata( Map<String, Object> metadata ) {
        this.metadata = metadata;
    }


    public String getEntityPath( String url_base, org.apache.usergrid.persistence.Entity entity ) {
        String entity_uri = null;
        if ( !Application.ENTITY_TYPE.equals( entity.getType() ) ) {
            entity_uri = createPath( pluralize( entity.getType() ), entity.getUuid().toString() );
        }
        else {
            entity_uri = createPath();
        }
        return entity_uri;
    }

//TODO: figure out what this does.
//    public void prepareEntities() {
//        if ( uri != null ) {
//            String url_base = serverEnvironmentProperties.getApiBase();
//            if ( entities != null ) {
//                for ( Entity entity : entities ) {
//                    String entity_uri = getEntityPath( url_base, entity );
//                    entity.setMetadata( "uri", entity_uri );
//                    entity.setMetadata( "path", path + "/" + entity.getUuid() );
//                }
//            }
//        }
//    }


    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }


    @JsonAnySetter
    public void setProperty( String key, Object value ) {
        properties.put( key, value );
    }


    /**
     * Create a path
     *
     * @return `
     */
    private String createPath( String... suffixes ) {

        StringBuilder builder = new StringBuilder();

        builder.append( serverEnvironmentProperties.getApiBase() );
        if ( !serverEnvironmentProperties.getApiBase().endsWith( "/" ) ) {
            builder.append( "/" );
        }
        builder.append( organization );
        builder.append( "/" );
        builder.append( applicationName );

        if ( suffixes.length == 0 ) {
            return builder.toString();
        }


        for ( String current : suffixes ) {
            if ( current == null ) {
                continue;
            }

            if ( !current.startsWith( "/" ) ) {
                builder.append( "/" );
            }
            builder.append( current );
        }

        return builder.toString();
    }
}
