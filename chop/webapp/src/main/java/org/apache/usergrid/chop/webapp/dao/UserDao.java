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
package org.apache.usergrid.chop.webapp.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.apache.usergrid.chop.webapp.elasticsearch.Util;
import org.apache.usergrid.chop.webapp.service.shiro.ShiroRealm;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;


/**
 * User persistence operations
 */
@Singleton
public class UserDao extends Dao {

    public static final String DAO_INDEX_KEY = "users";
    public static final String DAO_TYPE_KEY = "user";

    private static final int MAX_RESULT_SIZE = 10000;


    /**
     * @param elasticSearchClient   elasticsearch client to use for CRUD of User
     */
    @Inject
    public UserDao( IElasticSearchClient elasticSearchClient ) {
        super( elasticSearchClient );
    }


    /**
     * Stores a new user, username is used as ID, since it should be unique
     *
     * @param user  User to save
     * @return      whether the operation succeeded
     * @throws Exception
     */
    public boolean save( User user ) throws IOException {

        IndexResponse response = elasticSearchClient.getClient()
                .prepareIndex( DAO_INDEX_KEY, DAO_TYPE_KEY, user.getUsername() )
                .setRefresh( true )
                .setSource(
                        jsonBuilder()
                                .startObject()
                                .field( "password", user.getPassword() )
                                .endObject()
                )
                .execute()
                .actionGet();

        return response.isCreated();
    }

    /**
     * Gets a User object containing the information for given username
     *
     * @param username  queried User's username
     * @return          User object or null if no user with <code>username</code> exists
     */
    public User get( String username ) {

        SearchResponse response = elasticSearchClient.getClient()
                .prepareSearch( DAO_INDEX_KEY )
                .setTypes( DAO_TYPE_KEY )
                .setQuery( termQuery( "_id", username ) )
                .execute()
                .actionGet();

        SearchHit hits[] = response.getHits().hits();

        return hits.length > 0 ? toUser( hits[ 0 ] ) : null;
    }


    /**
     * @return  A list of all stored Users in elasticsearch
     */
    public List<User> getList() {
        SearchResponse response = elasticSearchClient.getClient()
                .prepareSearch( DAO_INDEX_KEY )
                .setTypes( DAO_TYPE_KEY )
                .setSize( MAX_RESULT_SIZE )
                .execute()
                .actionGet();

        ArrayList<User> users = new ArrayList<User>();

        for ( SearchHit hit : response.getHits().hits() ) {
            // Show all users to only admin user
            if ( ShiroRealm.isAuthenticatedUserAdmin() ) {
                users.add( toUser( hit ) );
            }
            else {
                users.add( get( ShiroRealm.getAuthenticatedUser() ) );
            }
        }

        return users;
    }


    /**
     * Creates a User object from given elastic search query
     *
     * @param hit   Elasticsearch <code>SearchHit</code> that holds the user information
     * @return      <code>User</code> object corresponding to the given query result
     */
    private static User toUser( SearchHit hit ) {
        Map<String, Object> json = hit.getSource();

        return new User(
                hit.getId(),
                Util.getString( json, "password" )
        );
    }


    /**
     * Deletes a <code>User</code> with given <code>username</code> from storage
     *
     * @param username  Name of the user which will be deleted
     * @return          whether such a user existed in storage
     */
    public boolean delete( String username ) {

        DeleteResponse response = elasticSearchClient.getClient()
                .prepareDelete( DAO_INDEX_KEY, DAO_TYPE_KEY, username )
                .setRefresh( true )
                .execute()
                .actionGet();

        return response.isFound();
    }
}