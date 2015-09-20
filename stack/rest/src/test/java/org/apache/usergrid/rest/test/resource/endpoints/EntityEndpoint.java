/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.rest.test.resource.endpoints;

import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class EntityEndpoint extends NamedResource {

    public EntityEndpoint(String identifier, ClientContext context, UrlResource parent) {
        super(identifier, context, parent);
    }


    /**
     *
     * GET a single entity
     *
     * @return Entity
     *
     *
     * entity = app.collection("users").uniqueID("fred").get(); //return one entity
     * GET /users/fred
     *
     * entity = app.collection("users").entity(entity).get(); //return one entity
     * GET /users/username_in_entity_obj
     *
     */
    public Entity get(){
        return get(true);
    }

    public Entity get(final boolean useToken){
        return get(useToken,null);
    }
    public Entity get(final Token token){
        return get(true,token);
    }

    public Entity get(final boolean useToken, final Token token){
        WebTarget resource  = getTarget( useToken, token );
        ApiResponse response = resource.request()
            .accept( MediaType.APPLICATION_JSON )
            .get(ApiResponse.class);

        return new Entity(response);
    }

    //For testing purposes only
    public Entity get(QueryParameters parameters, final boolean useToken){
        WebTarget resource  = getTarget(useToken);
        resource = addParametersToResource(resource, parameters);
        ApiResponse response = resource.request()
            .accept( MediaType.APPLICATION_JSON )
            .get(ApiResponse.class);

        return new Entity(response);
    }


    /**
     * DELETE a single entity
     *
     *
     * app.collection("users").entity(entity).delete();
     * DELETE /users/username?token=<token>
     *
     * app.collection("users").entity(entity).delete(false);
     * DELETE /users/uuid
     */
    public ApiResponse delete(){
        return delete(true);
    }

    public ApiResponse delete(final boolean useToken){
        WebTarget resource  = getTarget( useToken );
        return resource.request()
            .accept( MediaType.APPLICATION_JSON )
            .delete(ApiResponse.class);
    }

    /**
     * Put the entity to the collection
     * @param entity
     * @return
     *
     * app.collection("users").entity(entity).put(entity);
     * PUT /users/uuid {}
     *
     * app.collection("users").uniqueID("fred").put(entity);
     * PUT /users/fred {"color":"red"}
     */
    public Entity put(Entity entity){
        ApiResponse response = getTarget( true ).request()
            .accept(MediaType.APPLICATION_JSON)
            .put( javax.ws.rs.client.Entity.json(entity), ApiResponse.class );
        return new Entity( response);
    }


    /**
     * POST with no payload
     *
     * app.collection("users").uniqueID("fred").connection("following").collection("users").uniqueID("barney").post();
     * POST /users/fred/following/users/barney?token=<token>
     *
     * app.collection("users").uniqueID("fred").connection("following").collection("users").uniqueID("barney").post(false);
     * POST /users/fred/following/users/barney
     *
     */
    public Entity post(){
        return post(true);
    }

    public Entity post(final boolean useToken){
        WebTarget resource  = getTarget(useToken);
        ApiResponse response = resource.request()
            .accept(MediaType.APPLICATION_JSON)
            .post( javax.ws.rs.client.Entity.json(null), ApiResponse.class );

        return new Entity( response);
    }

    /**
     *
     * app.collection("users").uniqueID("fred").connection("following).get();
     * GET /users/fred/following
     *
     * app.collection("users").uniqueID("fred").connection("following").collection("users").uniqueID("barney").post();
     * POST /users/fred/following/users/barney?token=<token>
     *
     * app.collection("users").uniqueID("fred").connection().collection("users").uniqueID("barney").post();
     * POST /users/fred/following/users/barney?token=<token>
     *
     */
    public CollectionEndpoint connection(final String connection,final String collection) {
        return new CollectionEndpoint(connection+"/"+collection, context, this);
    }
    public CollectionEndpoint connection(final String connection) {
        return new CollectionEndpoint(connection, context, this);

    }
    public CollectionEndpoint collection(final String identifier) {
        return new CollectionEndpoint(identifier, context, this);
    }
    public CollectionEndpoint connection(){
        return new CollectionEndpoint("", context, this);
    }


    public CollectionEndpoint activities() {
        return collection("activities");
    }
}
