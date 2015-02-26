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
package org.apache.usergrid.rest.test.resource2point0.endpoints;

import com.sun.jersey.api.client.WebResource;
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;
import org.apache.usergrid.services.ServiceParameter;
import org.apache.usergrid.utils.StringUtils;

import javax.ws.rs.core.MediaType;
import java.util.*;


/**
 * //myorg/myapp/mycollection
 */
public class CollectionEndpoint extends NamedResource {

    protected List<String> acceptHeaders = new ArrayList<String> ();

    public CollectionEndpoint(String name, ClientContext context, UrlResource parent) {
        super(name, context, parent);
    }

    public EntityEndpoint uniqueID(final String identifier){
        return new EntityEndpoint(identifier, context, this);
    }

    public EntityEndpoint entity(final Entity entity){
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
     *
     * app.collection("users").uniqueID("fred").connection("following").collection("users").uniqueID("barney").post();
     * POST /users/fred/following/users/barney?token=<token>
     *
     * app.collection("users").uniqueID("fred").connection().collection("users").uniqueID("barney").post();
     * POST /users/fred/groups/theitcrowd?token=<token>
     */
    public CollectionEndpoint collection(final String identifier){
        return new CollectionEndpoint(identifier, context, this);
    }



    /**
     * Get a list of entities
     * @return
     *
     * @usage
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
     */
    public Collection get(){
        return get(null, true);
    }

    public Collection get( final QueryParameters parameters ){
        return get(parameters, true);
    }

    public Collection get(final QueryParameters parameters, final boolean useToken){

        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
           acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        WebResource resource  = getResource(useToken);
        resource = addParametersToResource(resource, parameters);
        ApiResponse response = resource.type( MediaType.APPLICATION_JSON_TYPE ).accept(acceptHeader)
                .get(ApiResponse.class);

        return new Collection(response);
    }

    /**
     * Gets the next page using only default settings with the passed in collection.
     * @param collection
     * @param useToken
     * @return
     *
     * @usage
     * Collection usersCollection =  app.collection("users").get();
     * //iterate through the collection
     * while(usersCollection.hasNext()){
     *  Entity bob = usersCollection.next();
     *     assert("blah",bob.get("words"));
     * }     *
     * usersCollection = app.collections("users").getNextPage(usersCollection.cursor);
     */
    //TODO: add queryParameters here
    public Collection getNextPage(Collection collection, QueryParameters passedParameters ,final boolean useToken) {
        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        WebResource resource = getResource(useToken);
        QueryParameters queryParameters = passedParameters;
        if( queryParameters == null){
            queryParameters = new QueryParameters();
        }

        queryParameters.setCursor(collection.getCursor());
        resource = addParametersToResource(resource, queryParameters);

        ApiResponse response = resource.type( MediaType.APPLICATION_JSON_TYPE ).accept(acceptHeader)
                .get(ApiResponse.class);

        return new Collection(response);
    }

    /**
     * DELETE on a collection endpoint with query (use DELETE on entity for single entity delete)
     *
     * @return ApiResponse
     *
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
     */
    public ApiResponse delete( final QueryParameters parameters ){
        return delete(parameters, true);
    }

    public ApiResponse delete(final QueryParameters parameters, final boolean useToken){
        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }

        WebResource resource  = getResource(useToken);
        resource = addParametersToResource(resource, parameters);
        return resource.type( MediaType.APPLICATION_JSON_TYPE ).accept(acceptHeader)
                .delete(ApiResponse.class);
    }

    /**
     * Post an entity to a collection
     * @param payload
     * @return
     *
     * app.collection("users").post(entity);
     * POST /users {"color","red"}
     *
     */
    public Entity post(Entity payload){
        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }
        ApiResponse response = getResource(true).type( MediaType.APPLICATION_JSON_TYPE ).accept(acceptHeader)
                .post(ApiResponse.class, payload);
        return new Entity(response);
    }

    public Entity post(){
        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }
        ApiResponse response = getResource(true).type( MediaType.APPLICATION_JSON_TYPE ).accept(acceptHeader)
                .post(ApiResponse.class);
        return new Entity(response);
    }

    public ApiResponse post(List<Entity> entityList){
        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }
        ApiResponse response = getResource(true).type( MediaType.APPLICATION_JSON_TYPE ).accept(acceptHeader)
                .post(ApiResponse.class,entityList);
        return response;
    }

    /**
     * PUT a payload to a collection
     *
     * app.collection("users").put(entity, param);
     * PUT /users?ql=select * where created > 0&token=<token>
     *
     * app.collection("users").put(entity, false, param);
     * PUT /users?ql=select * where created > 0
     *
     */
    public ApiResponse put( final QueryParameters parameters, Entity entity ){
        return put(parameters, true, entity);
    }

    public ApiResponse put(final QueryParameters parameters, final boolean useToken, Entity entity){
        String acceptHeader = MediaType.APPLICATION_JSON;
        if (this.acceptHeaders.size() > 0) {
            acceptHeader = StringUtils.join(this.acceptHeaders, ',');
        }
        WebResource resource  = getResource(useToken);
        addParametersToResource(getResource(), parameters);
        return resource.type( MediaType.APPLICATION_JSON_TYPE ).accept(acceptHeader)
                .put(ApiResponse.class, entity);
    }





}
