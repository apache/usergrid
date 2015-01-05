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


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.utils.MapUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;

import static org.junit.Assert.assertEquals;
import static org.apache.usergrid.utils.MapUtils.hashMap;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class AndOrQueryTest extends AbstractRestIT {

    @Test //USERGRID-900
    public void queriesWithAndPastLimit() throws IOException {


        long created = 0;
      Entity actor = new Entity();
      actor.put("displayName", "Erin");
      Entity props = new Entity();
        props.put( "actor", actor );
        props.put( "verb", "go" );
        props.put( "content", "bragh" );


        for ( int i = 0; i < 20; i++ ) {
            if ( i < 10 ) {
                props.put( "madeup", false );
            }
            else {
                props.put( "madeup", true );
            }

            props.put( "ordinal", i );
            Entity activity = this.app().collection("activities").post(props);
            if ( i == 0 ) {
                created = Long.parseLong(activity.get("created").toString());
            }
        }

        this.refreshIndex();

        String errorQuery = "select * where created >= " + created + "AND madeup = true";
      QueryParameters params= new QueryParameters();
      params.setQuery(errorQuery);
       Collection activities=this.app().collection("activities").get(params);

        assertEquals( 10, activities.response.getEntityCount() );
    }


    @Test //USERGRID-1475
    public void displayFullQueriesInLimit() throws IOException {

      int numValuesTested = 20;
      Entity actor = new Entity();
      actor.put("displayName", "Erin");
      Entity props = new Entity();
      props.put( "actor", actor );
      props.put( "verb", "go" );
      props.put( "content", "bragh" );


      for ( int i = 0; i < numValuesTested; i++ ) {
        if ( i < numValuesTested/2 ) {
          props.put( "verb", "go" );
        }
        else {
          props.put( "verb", "stop" );
        }

        props.put( "ordinal", i );
        this.app().collection("activities").post(props);
      }


        this.refreshIndex();

        String query = "select * where not verb = 'go'";
      QueryParameters params= new QueryParameters();
      params.setQuery(query);
      Collection activities=this.app().collection("activities").get(params);

      assertEquals( numValuesTested/2, activities.response.getEntityCount() );
      List entities = activities.response.getEntities();

        for ( int i = 0; i < numValuesTested/2; i++ ) {
            assertEquals(numValuesTested - i, Integer.parseInt(((LinkedHashMap<String, Object>) entities.get(i)).get("ordinal").toString()));
            assertEquals( "stop", ((LinkedHashMap<String, Object>) entities.get(i)).get("verb").toString() );
        }
    }
  

    @Test //USERGRID-1615
    public void queryReturnCount() throws Exception {


        int numValuesTested = 20;

      Entity actor = new Entity();
      actor.put("displayName", "Erin");
      Entity props = new Entity();
      props.put( "actor", actor );
      props.put( "verb", "go" );
      props.put( "content", "bragh" );
      Entity[] correctValues = new Entity[numValuesTested];
      for(int i=0; i< numValuesTested; i++){
        props.put("ordinal", i);
        correctValues[i] = this.app().collection("activities").post(props);
      }
      this.refreshIndex();

      String inCorrectQuery = "select * where ordinal >= 10 order by ordinal asc";
      QueryParameters params = new QueryParameters();
      params.setQuery(inCorrectQuery);
      Collection activities=this.app().collection("activities").get(params);

      assertEquals( 10, activities.response.getEntityCount() );

//      verificationOfQueryResults( "activities", (Entity[])ArrayUtils.subarray(correctValues, 10, 19), false, inCorrectQuery );
      List entities = activities.response.getEntities();

      for ( int i = 0; i < 10; i++ ) {
        assertEquals(10+i, Integer.parseInt(((LinkedHashMap<String, Object>) entities.get(i)).get("ordinal").toString()));
//        assertEquals( "stop", ((LinkedHashMap<String, Object>) entities.get(i)).get("verb").toString() );
      }

    }


    @Test //Check to make sure that asc works
    public void queryCheckAsc() throws Exception {
      int numOfEntities = 10;
        String collectionName = "imagination" + RandomStringUtils.randomAlphabetic( 5 );

      Entity[] correctValues = new Entity[numOfEntities];
      Entity props = new Entity();
      props.put( "WhoHelpedYou", "Ruff" );
      for(int i=0; i< numOfEntities; i++){
        props.put("ordinal", i);
        correctValues[i] = this.app().collection(collectionName).post(props);
      }

        this.refreshIndex(  );

        String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 10 "
                + "or WhoHelpedYou eq 'Ruff' ORDER BY Ordinal asc";

        int totalEntitiesContained = verificationOfQueryResults( collectionName, correctValues, false, inquisitiveQuery );

        assertEquals( numOfEntities, totalEntitiesContained );
    }


//    @Ignore("Test to make sure all 1000 exist with a regular query")
    @Test
    public void queryReturnCheck() throws Exception {
        int numOfEntities = 20;

      String collectionName = "imagination" + RandomStringUtils.randomAlphabetic( 5 );

      Entity[] correctValues = new Entity[numOfEntities];
      Entity props = new Entity();
      props.put( "WhoHelpedYou", "Ruff" );
      for(int i=0; i< numOfEntities; i++){
        props.put("ordinal", i);
        correctValues[i] = this.app().collection(collectionName).post(props);
      }

      this.refreshIndex(  );

        String inquisitiveQuery = "select * where ordinal >= 0 and ordinal <= 2000 or WhoHelpedYou = 'Ruff'";

        int totalEntitiesContained = verificationOfQueryResults( collectionName, correctValues, true, inquisitiveQuery );

        assertEquals( numOfEntities, totalEntitiesContained );
    }


    @Test
    public void queryReturnCheckWithShortHand() throws Exception {
      int numOfEntities = 20;

      String collectionName = "imagination" + RandomStringUtils.randomAlphabetic( 5 );

      Entity[] correctValues = new Entity[numOfEntities];
      Entity props = new Entity();
      props.put( "WhoHelpedYou", "Ruff" );
      for(int i=0; i< numOfEntities; i++){
        props.put("ordinal", i);
        correctValues[i] = this.app().collection(collectionName).post(props);
      }

      this.refreshIndex(  );

      String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff'";

      int totalEntitiesContained = verificationOfQueryResults( collectionName, correctValues, true, inquisitiveQuery );

      assertEquals( numOfEntities, totalEntitiesContained );

    }

  public int verificationOfQueryResults( String collectionName, Entity[] correctValues, boolean reverse, String checkedQuery )
      throws Exception {

    int totalEntitiesContained = 0;
    QueryParameters params = new QueryParameters();
    params.setQuery(checkedQuery);
    Collection checkedNodes = this.app().collection(collectionName).get(params);

    while ( correctValues.length != totalEntitiesContained )//correctNode.get("entities") != null)
    {
      totalEntitiesContained += checkedNodes.response.getEntityCount();
      if ( !reverse ) {
        for ( int index = 0; index < checkedNodes.response.getEntityCount(); index++ ) {
          assertEquals( correctValues[index].get("uuid"),
              ((LinkedHashMap)checkedNodes.response.getEntities().get( index )).get("uuid") );
        }
      }
      else {
        for ( int index = 0; index < checkedNodes.response.getEntityCount(); index++ ) {
          assertEquals( correctValues[correctValues.length - 1 - index].get("uuid"),
              ((LinkedHashMap)checkedNodes.response.getEntities().get( index )).get("uuid") );
        }
      }

      if ( checkedNodes.getCursor() != null ) {
        checkedNodes = this.app().getNextPage(checkedNodes,true);
      }

      else {
        break;
      }
    }
    return totalEntitiesContained;
  }

}
