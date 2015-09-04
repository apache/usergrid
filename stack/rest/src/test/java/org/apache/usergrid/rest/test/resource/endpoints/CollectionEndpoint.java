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
package org.apache.usergrid.rest.test.resource.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.state.ClientContext;
import org.apache.usergrid.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * //myorg/myapp/mycollection
 */
public class CollectionEndpoint extends NamedResource {
    private static final Logger logger = LoggerFactory.getLogger(CollectionEndpoint.class);

    protected List<String> acceptHeaders = new ArrayList<String> ();
    private String matrix;

    public CollectionEndpoint(String name, ClientContext context, UrlResource parent) {
        super(name, context, parent);
    }

    public EntityEndpoint uniqueID(final String identifier){
        return new EntityEndpoint(identifier, context, this);
    }

    public EntityEndpoint entity(final org.apache.usergrid.rest.test.resource.model.Entity entity){
        String identifier = (String) entity.get("uuid");
        return entity(identifier);
    }

    public EntityEndpoint entity(final UUID identifier ){
        return entity(identifier.toString());
    }


    public EntityEndpoint entity(final String identifier ){
        return uniqueID(identifier);
    }

    public CollectionEndpoint withAcceptHeader(final String acceptHeader) {
        this.acceptHeaders.add(acceptHeader);
        return this;
    }

    /**
     * <pre>
     * app.collection("users").uniqueID("fred").connection("following").collection("users").uniqueID("barney").post();
     * POST /users/fred/following/users/barney?token=<token>
     *
     * app.collection("users").uniqueID("fred").connection().collection("users").uniqueID("barney").post();
     * POST /users/fred/groups/theitcrowd?token=<token>
     * </pre>
     */
    public CollectionEndpoint collection(final String identifier){
        return new CollectionEndpoint(identifier, context, this);
    }



    /**
     * Get a list of entities.
     *
     * <pre>
     * //with token
     * app.collection("users").get(); //return entity
     * GET /users?token=<token>
     *
     * //with query and token
     * collection = app.collection("users").get(queryparam); //return collection (list of entities)
     * GET /users?ql=select * where created > 0&token=<token>
     *
     * //with query and no token
     * collection = app.collection("users").get(queryparam, false); //return collection (list of entities)
     * GET /users?ql=select * where created > 0
     *
     * //with no query and no token
     * collection = app.collection("users").get(null, false); //return collection (list of entities)
     * GET /users
     *
     * collection = app.collection("users").get(collection);
     * <pre>
     */
    public Collection get(){
        return new Collection( get(org.apache.usergrid.rest.test.resource.model.ApiResponse.class,null,true));
    }

    public Collection get( final org.apache.usergrid.rest.test.resource.model.QueryParameters parameters ){
        return get(parameters, true);
    }

    public Collection get(
        final org.apache.usergrid.rest.test.resource.model.QueryParameters parameters,
        final boolean useToken){

        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
           acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        WebTarget resource  = getTarget( useToken );
        resource = addParametersToResource(resource, parameters);

        logger.info("PATH is "+ resource.getUri().getRawPath()+"?"+resource.getUri().getRawQuery());
        // use string type so we can log actual response from server
        String responseString = resource.request()
            .accept( acceptHeader )
            .get(String.class);

        logger.debug("Response from get: " + responseString);

        ObjectMapper mapper = new ObjectMapper();
        org.apache.usergrid.rest.test.resource.model.ApiResponse response;
        try {
            response = mapper.readValue(
                new StringReader(responseString), org.apache.usergrid.rest.test.resource.model.ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        return new Collection(response);
    }

    /**
     * Gets the next page using only default settings with the passed in collection.
     *
     * <pre>
     * Collection usersCollection =  app.collection("users").get();
     * //iterate through the collection
     * while(usersCollection.hasNext()){
     *  Entity bob = usersCollection.next();
     *     assert("blah",bob.get("words"));
     * }
     * usersCollection = app.collections("users").getNextPage(usersCollection.cursor);
     * </pre>
     */
    //TODO: add queryParameters here
    public Collection getNextPage(Collection collection,
             org.apache.usergrid.rest.test.resource.model.QueryParameters passedParameters ,
             final boolean useToken) {

        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        WebTarget resource = getTarget( useToken );
        org.apache.usergrid.rest.test.resource.model.QueryParameters queryParameters = passedParameters;
        if( queryParameters == null){
            queryParameters = new org.apache.usergrid.rest.test.resource.model.QueryParameters();
        }

        queryParameters.setCursor(collection.getCursor());
        resource = addParametersToResource(resource, queryParameters);

        org.apache.usergrid.rest.test.resource.model.ApiResponse response =
            resource.request()
            .accept( acceptHeader )
            .get(org.apache.usergrid.rest.test.resource.model.ApiResponse.class);

        return new Collection(response);
    }

    /**
     * DELETE on a collection endpoint with query (use DELETE on entity for single entity delete).
     *
     * <pre>
     * //with token
     * app.collection("users").delete(parameters);
     * DELETE /users?ql=select * where created > 0&token=<token>
     *
     * //without token
     * app.collection("users").delete(parameters, false);
     * DELETE /users?ql=select * where created > 0
     *
     * app.collection("users").delete(null, false);
     * DELETE /users
     * </pre>
     */
    public org.apache.usergrid.rest.test.resource.model.ApiResponse delete(
        final org.apache.usergrid.rest.test.resource.model.QueryParameters parameters ){
        return delete(parameters, true);
    }

    public org.apache.usergrid.rest.test.resource.model.ApiResponse delete(
        final org.apache.usergrid.rest.test.resource.model.QueryParameters parameters, final boolean useToken) {

        String acceptHeader = MediaType.APPLICATION_JSON;

        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        WebTarget resource  = getTarget( useToken );
        resource = addParametersToResource(resource, parameters);
        return resource.request()
            .accept( acceptHeader )
            .delete(org.apache.usergrid.rest.test.resource.model.ApiResponse.class);
    }

    /**
     * Post an entity to a collection.
     *
     * <pre>
     * app.collection("users").post(entity);
     * POST /users {"color","red"}
     * </pre>
     */
    public org.apache.usergrid.rest.test.resource.model.Entity post(
        org.apache.usergrid.rest.test.resource.model.Entity payload){

        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        // use string type so we can log actual response from server
        String responseString = getTarget( true )
            .request()
            .accept(acceptHeader)
            .post( javax.ws.rs.client.Entity.json( payload ), String.class);

        logger.debug("Response from post: " + responseString);

        ObjectMapper mapper = new ObjectMapper();
        org.apache.usergrid.rest.test.resource.model.ApiResponse response;
        try {
            response = mapper.readValue( new StringReader(responseString),
                org.apache.usergrid.rest.test.resource.model.ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        return new org.apache.usergrid.rest.test.resource.model.Entity(response);
    }

    public org.apache.usergrid.rest.test.resource.model.Entity post() {

        String acceptHeader = MediaType.APPLICATION_JSON;

        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        // use string type so we can log actual response from server
        String responseString = getTarget( true )
            .request()
            .accept( acceptHeader )
            .post( javax.ws.rs.client.Entity.json( null ), String.class);

        logger.debug("Response from post: " + responseString);

        ObjectMapper mapper = new ObjectMapper();
        org.apache.usergrid.rest.test.resource.model.ApiResponse response;
        try {
            response = mapper.readValue( new StringReader(responseString),
                org.apache.usergrid.rest.test.resource.model.ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        return new org.apache.usergrid.rest.test.resource.model.Entity(response);
    }

    public org.apache.usergrid.rest.test.resource.model.ApiResponse post(
        List<org.apache.usergrid.rest.test.resource.model.Entity> entityList) {

        String acceptHeader = MediaType.APPLICATION_JSON;

        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        // use string type so we can log actual response from server
        String responseString = getTarget( true )
            .request()
            .accept( acceptHeader )
            .post( javax.ws.rs.client.Entity.json( entityList ), String.class);

        logger.debug("Response from post: " + responseString);

        ObjectMapper mapper = new ObjectMapper();
        org.apache.usergrid.rest.test.resource.model.ApiResponse response;
        try {
            response = mapper.readValue( new StringReader(responseString),
                org.apache.usergrid.rest.test.resource.model.ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        return response;
    }

    /**
     * PUT a payload to a collection.
     *
     * <pre>
     * app.collection("users").put(entity, param);
     * PUT /users?ql=select * where created > 0&token=<token>
     *
     * app.collection("users").put(entity, false, param);
     * PUT /users?ql=select * where created > 0
     * </pre>
     */
    public org.apache.usergrid.rest.test.resource.model.ApiResponse put(
        final org.apache.usergrid.rest.test.resource.model.QueryParameters parameters,
        org.apache.usergrid.rest.test.resource.model.Entity entity ){
        return put(parameters, true, entity);
    }

    public org.apache.usergrid.rest.test.resource.model.ApiResponse put(
        final org.apache.usergrid.rest.test.resource.model.QueryParameters parameters,
        final boolean useToken,
        org.apache.usergrid.rest.test.resource.model.Entity entity) {

        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        WebTarget resource  = getTarget( useToken );
        addParametersToResource(getTarget(), parameters);

        // use string type so we can log actual response from server
        String responseString = resource.request()
            .accept(acceptHeader)
            .post( javax.ws.rs.client.Entity.json( entity ), String.class);

        logger.debug("Response from put: " + responseString);

        ObjectMapper mapper = new ObjectMapper();
        org.apache.usergrid.rest.test.resource.model.ApiResponse response;
        try {
            response = mapper.readValue( new StringReader(responseString),
                org.apache.usergrid.rest.test.resource.model.ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        return response;
    }

    public CollectionEndpoint matrix(
        org.apache.usergrid.rest.test.resource.model.QueryParameters parameters) {
        this.matrix = getMatrixValue(parameters);
        return this;
    }

    @Override
    public String getMatrix(){
        return matrix != null ? matrix : "";
    }
}
