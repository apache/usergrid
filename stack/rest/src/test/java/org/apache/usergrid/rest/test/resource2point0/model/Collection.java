/**
 * Created by ApigeeCorporation on 12/4/14.
 */
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

package org.apache.usergrid.rest.test.resource2point0.model;

import java.util.Iterator;
import java.util.Map;


/**
 * A stateful iterable collection response. Used to dole out entities in iterable form
 *
 */
public class Collection implements Iterable<Entity>, Iterator<Entity> {


    private String cursor;

    private Iterator entities;

    private ApiResponse response;


    /**
     * Collection usersCollection =  app.collection("users").get();
     * while(usersCollection.hasNext()){
     *  Entity bob = usersCollection.next();
     *     assert("blah",bob.get("words"));
     * }
     * QueryParams = new QueryParams(usersCollection.cursor)
     * app.collections("users").get(queryParams);
     *
     * usersCollection = app.collections("users").getNextPage(usersCollection.cursor);
     *
     * Use the factory method instead
     * @param response
     */
    public Collection(ApiResponse response) {
        this.response = response;
        this.cursor = response.getCursor();
        entities = response.getEntities().iterator();
    }

    public ApiResponse getResponse(){
        return response;
    }

    @Override
    public Iterator iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        return entities.hasNext();
    }

    public String getCursor(){
        return cursor;
    }


    @Override
    public Entity next() {
        return new Entity( ( Map<String, Object> ) entities.next() );
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is unsupported" );
    }
}

