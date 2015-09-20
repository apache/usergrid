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
package org.apache.usergrid.rest.applications.queries;


import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Test;

import javax.ws.rs.ClientErrorException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Test for exceptions resulting from invalid query syntax
 */
public class BadGrammarQueryTest extends QueryTestBase {

    /**
     * We should get an exception if the clause contains
     * an invalid operator.
     * (eg. "name != 'go'" instead of "NOT name = 'go'")
     * @throws IOException
     */
    @Test
    public void exceptionOnInvalidOperator() throws IOException {

        int numOfEntities = 1;
        String collectionName = "things";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        //Issue an invalid query
        String query = "select * where name != 'go'";
        try {

            QueryParameters params = new QueryParameters().setQuery(query);
            this.app().collection(collectionName).get(params);
            fail("This should throw an exception");
        } catch (ClientErrorException uie) {
            //Check for an exception
            assertEquals(400, uie.getResponse().getStatus());
        }
    }

    /**
     * We should get an exception if the clause contains
     * a string that is surrounded by double-quotes
     * @throws IOException
     */
    @Test
    public void exceptionOnDoubleQuotes() throws IOException {

        int numOfEntities = 1;
        String collectionName = "things";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        //Issue an invalid query
        String query = "select * where NOT name = \"go\"";
        try {

            QueryParameters params = new QueryParameters().setQuery(query);
            this.app().collection(collectionName).get(params);
            fail("This should throw an exception");
        } catch (ClientErrorException uie) {
            //Check for an exception
            assertEquals(400, uie.getResponse().getStatus());
        }
    }

    /**
     * We should get an exception if the clause contains
     * a string that is not properly quoted
     * @throws IOException
     */
    @Test
    public void exceptionOnMissingQuotes() throws IOException {

        int numOfEntities = 1;
        String collectionName = "things";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        //Issue an invalid query
        String query = "select * where name != go";
        try {

            QueryParameters params = new QueryParameters().setQuery(query);
            this.app().collection(collectionName).get(params);
            fail("This should throw an exception");
        } catch (ClientErrorException uie) {
            //Check for an exception
            assertEquals(400, uie.getResponse().getStatus());
        }
    }

    /**
     * We should get an exception if the property name
     * is missing from the clause
     * @throws IOException
     */
    @Test
    public void exceptionOnMissingProperty() throws IOException {

        int numOfEntities = 1;
        String collectionName = "things";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        //Issue an invalid query
        String query = "select * where != 'go'";
        try {

            QueryParameters params = new QueryParameters().setQuery(query);
            this.app().collection(collectionName).get(params);
            fail("This should throw an exception");
        } catch (ClientErrorException uie) {
            //Check for an exception
            assertEquals(400, uie.getResponse().getStatus());
        }
    }

    /**
     * We should get an exception if the property value
     * is missing from the clause
     * @throws IOException
     */
    @Test
    public void exceptionOnMissingPropertyValue() throws IOException {

        int numOfEntities = 1;
        String collectionName = "things";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        //Issue an invalid query
        String query = "select * where name != ";
        try {

            QueryParameters params = new QueryParameters().setQuery(query);
            this.app().collection(collectionName).get(params);
            fail("This should throw an exception");
        } catch (ClientErrorException uie) {
            //Check for an exception
            assertEquals(400, uie.getResponse().getStatus());
        }
    }

    /**
     * We should get an exception if the operator is missing
     * from the clause
     * @throws IOException
     */
    @Test
    public void exceptionOnMissingOperator() throws IOException {

        int numOfEntities = 1;
        String collectionName = "things";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        //Issue an invalid query
        String query = "select * where name 'go' ";
        try {

            QueryParameters params = new QueryParameters().setQuery(query);
            this.app().collection(collectionName).get(params);
            fail("This should throw an exception");
        } catch (ClientErrorException uie) {
            //Check for an exception
            assertEquals(400, uie.getResponse().getStatus());
        }
    }

    /**
     * Limit should be sent separately from the query,
     * else the query will return only entities with
     * a property named 'limit'
     * @throws IOException
     */
    @Test
    public void limitInQuery() throws IOException {

        int numOfEntities =1;
        String collectionName = "things";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);


        //Issue a query with the limit appended
        String query = "select * where limit = 5";
        QueryParameters params = new QueryParameters().setQuery(query);
        Collection collection = this.app().collection(collectionName).get(params);
        assertEquals(0, collection.getNumOfEntities());
    }

}
