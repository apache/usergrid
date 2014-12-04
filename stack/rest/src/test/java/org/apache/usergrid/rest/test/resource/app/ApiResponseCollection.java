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
package org.apache.usergrid.rest.test.resource.app;


import java.util.Iterator;

import org.apache.usergrid.rest.test.resource.RevisedApiResponse;
import org.apache.usergrid.rest.test.resource.CollectionResource;
import org.apache.usergrid.rest.test.resource.app.model.Entity;


/**
 * A stateful iterable collection response.  This is a "collection" of entities from our response that are easier
 * to work with. The Generic means that we can type cast the iterator
 *
 * Keep generics? Maybe just use entities for now
 * 1.) Primary key
 * 2.) Default data-> default data is different from type to type. (Groups would need path and title, Activities require actors...etc)
 * 3.) Things that you can do with them-> Groups create connections or something else. Adding users to a group. ( this can be boiled down to creating a connection )
 *
 * Two connecting builder patterns
 * 1. POST /collection/entity/verb (e.g. likes or following)/collection/entity  //connect any two entities
 *  - POST /users/fred/following/users/barney
 * 2. POST /collection/entity/collection/entity //for built in collections e.g. add user to group, add role to group, etc
 *  - POST users/fred/groups/funlovincriminals
 *
 * Two similar builder patterns for getting connected entities
 * 1. GET /users/fred/following
 * 2. GET /users/fred/groups
 *
 */
public class ApiResponseCollection implements Iterable, Iterator {

    private final CollectionResource sourceEndpoint;
    private RevisedApiResponse response;


    public Iterator entities;


    public ApiResponseCollection(final CollectionResource sourceCollection, final RevisedApiResponse response){
        this.response = response;
        this.sourceEndpoint = sourceCollection;
        this.entities = response.getEntities().iterator();
    }

    public RevisedApiResponse getResponse(){
        return response;
    }

    @Override
    public Iterator iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        if(!entities.hasNext()){
            advance();
        }

        return entities.hasNext();
    }


    @Override
    public Entity next() {
        return (Entity)entities.next();
    }


    /**
     * Go back to the endpoint and try to load the next page
     */
    private void advance(){

      //call the original resource for the next page.

        final String cursor = response.getCursor();

        //no next page
        if(cursor == null){
            return;
        }

        response = sourceEndpoint.withCursor( cursor ).getInternalResponse();
        this.entities = response.getEntities().iterator();
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is unsupported" );
    }
}
