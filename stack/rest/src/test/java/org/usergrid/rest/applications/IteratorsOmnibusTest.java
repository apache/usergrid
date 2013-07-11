package org.usergrid.rest.applications;

import org.apache.commons.lang.ArrayUtils;

import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.CustomCollection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.usergrid.utils.MapUtils.hashMap;
import javax.ws.rs.Path;





public class IteratorsOmnibusTest extends RestContextTest {


  /*complete*/
   /* Is this test simply just paging through connected entities Ask for a review here */
  @Test //USERGRID-266
  public void pageThroughConnectedEntities() {

    CustomCollection activities = collection("activities");

    long created = 0;
    int maxSize = 1500;
    long[] verifyCreated = new long[maxSize];
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();


    props.put("actor", actor);
    props.put("verb","go");

    for (int i = 0; i < maxSize; i++) {

      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
      verifyCreated[i] = activity.findValue("created").getLongValue();
      if (i == 0) { created = activity.findValue("created").getLongValue(); }
    }
    ArrayUtils.reverse(verifyCreated);
    String query = "select * where created >= " + created;
    

    JsonNode node = activities.query(query);
    int index = 0;
    while (node.get("entities").get("created") != null)
    {
      assertEquals(10,node.get("entities").size());

      int curSize = maxSize -(10* (index+1));
      index++;
      for(int i = 0; i < 10; i++) {
          assertEquals(verifyCreated[curSize],node.get("entities").get(i).get("created").getLongValue());
          curSize++;

      }
      if(node.get("cursor") != null)
        node = activities.query(query,"cursor",node.get("cursor").toString());

      else
        break;

    }

  }

  /*Ask For Review, does not seem broken*/
  @Test //USERGRID-545
  public void  putMassUpdateTest () {


    CustomCollection activities = collection("activities");


    Map<String, ?> payload = hashMap("name", "Austin");
    Map actor = hashMap("displayName", "Erin");
    Map newActor = hashMap("displayName","Bob");
    Map props = new HashMap();

    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");


    for (int i = 0; i < 5; i++) {

      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
    }

    String query = "select * ";

    JsonNode node  = activities.query(query);
    String uuid = node.get("entities").get(0).get("uuid").getTextValue();
    StringBuilder buf = new StringBuilder(uuid);


    activities.addToUrlEnd(buf);
    props.put("actor",newActor);
    node = activities.put(props);
    node = activities.query(query);

    assertEquals(6,node.get("entities").size());
  }



  /*complete*/
  @Test //USERGRID-900
  public void queriesWithAndPastLimit() {

    CustomCollection activities = collection("activities");

    long created = 0;
    long newlyCreated = 0;
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();

    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");


    for (int i = 0; i < 2000; i++) {
     if(i<1000)
       props.put("madeup",false);
     else
       props.put("madeup",true);

      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
      if (i == 0) { created = activity.findValue("created").getLongValue(); }
      if (i == 1000) { newlyCreated = activity.findValue("created").getLongValue();}
    }

    String query = "select * where created >= " + newlyCreated;
    String errorQuery = "select * where created >= " + created + "AND madeup = true";

    JsonNode node = activities.query(query);
    JsonNode incorrectNode = activities.query(errorQuery);

    assertEquals(node.get("entities").size(),incorrectNode.get("entities").size());

  }
  @Test //Ignore("needs to query username or email . ") //USERGRID-1222 /* this needs to query
  public void queryForUsername() {
    CustomCollection users = collection("users");

    Map props = new HashMap();

    props.put("username","Alica");
    users.create(props);

    props.put("username","Bob");
    users.create(props);

    JsonNode incorrect = users.query("select * where username = 'Alica'"); /* what exactly was this meant to do */
    JsonNode node = users.query("select *");

    assertNotNull(node.get("entities").get("username").get(0));


  }


  /*complete */
  @Test //USERGRID-1253
  public void pagingQueryReturnCorrectResults() {

    CustomCollection activities = collection("activities");

    long created = 0;
    int maxSize = 23;
    long[] verifyCreated = new long[maxSize];
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();

    props.put("actor", actor);
    props.put("content", "bragh");

    for (int i = 0; i < maxSize; i++) {

      if(i < 5)
        props.put("verb","stop");
      else
        props.put("verb","go");
      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
      verifyCreated[i] = activity.findValue("created").getLongValue();
      if (i == 5) { created = activity.findValue("created").getLongValue(); }
    }
    ArrayUtils.reverse(verifyCreated);

    String query = "select * where created >= " + created + "or verb = 'stop'";
    JsonNode incorrectNode = activities.query(query,"limit",Integer.toString(10));

    String correctQuery = "select * where verb = 'stop'";
    JsonNode node = activities.query(query);

    int totalEntitiesContained = totalNumOfEntities(node,correctQuery,activities,query,incorrectNode);

    assertEquals(maxSize, totalEntitiesContained);
  }



  /*complete*/
  @Test // USERGRID-1400
  public void orderByShouldNotAffectResults() {

    CustomCollection activities = collection("activities");

    long created = 0;
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();
    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");
    for (int i = 0; i < 20; i++) {
      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
      if (i == 5) { created = activity.findValue("created").getLongValue(); }
    }

    String query = "select * where created > " + created;
    JsonNode node = activities.query(query);
    assertEquals(10, node.get("entities").size());

    query = query + " order by created desc";
    node = activities.query(query);
    assertEquals(10, node.get("entities").size());
  }

  /*complete. ask for review*/

  @Ignore("Test uses up to many resources to be run reliably") // USERGRID-1401
  public void groupQueriesWithConsistantResults() {

    CustomCollection groups = context.application().collection("groups");

    int maxRangeLimit = 20;
    long[] index = new long[maxRangeLimit];
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();

    props.put("actor",actor);
    Map location = hashMap("latitude",37);
    location.put("longitude",-75);
    props.put ("location",location);
    props.put("verb", "go");
    props.put("content", "bragh");
    for (int i = 0; i < 20; i++) {
      String newPath = String.format("/kero" + i);
      props.put("path",newPath);
      props.put("ordinal", i);
      JsonNode activity = groups.create(props);
      index[i] = activity.findValue("created").getLongValue();
    }

    JsonNode node = null;
    for(int consistant = 0; consistant < 20; consistant++) {
      String query = "select * where location within 20000 of 37,-75 and created >= " + index[7] + " and " +
      "created < " + index[10];

      node = groups.query(query);
      assertEquals(3,node.get("entities").size());
    }



    for(int i = 2; i>0; i++)
      assertEquals(index[10-i],node.get("entities").get(i).get("created").getLongValue());
}


  /*completed*/

  @Ignore("Test uses up to many resources to run reliably") // USERGRID-1403
  public void groupQueriesWithGeoPaging() {

    CustomCollection groups = context.application().collection("groups");

    int maxRangeLimit = 2000;
    int minRangeLimit = 5;
    long[] index = new long[maxRangeLimit];
    Map actor = hashMap("displayName", "Erin");


    Map props = new HashMap();

    props.put("actor",actor);
    Map location = hashMap("latitude",37);
    location.put("longitude",-75);
    props.put ("location",location);
    props.put("verb", "go");
    props.put("content", "bragh");
    for (int i = 0; i < 1500; i++) {
      String newPath = String.format("/kero" + i);
      props.put("path",newPath);
      props.put("ordinal", i);
      JsonNode activity = groups.create(props);
      index[i] = activity.findValue("created").getLongValue();
    }

    String query = "select * where location within 20000 of 37,-75 and created > " + index[1301] + " and " +
        "created < " + index[1303] + "";
    JsonNode node = groups.query(query);
    assertEquals(1,node.get("entities").size());

    assertEquals(index[1302],node.get("entities").get(0).get("created").getLongValue());



  }

  /*complete*/
  @Test //USERGRID-1475
  public void displayFullQueriesInLimit() {

    CustomCollection activities = collection("activities");

    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();
    props.put("actor", actor);
    props.put("content","bragh");

    for (int i = 0; i < 20; i++) {

      if(i < 10)
        props.put("verb","go");
      else
        props.put("verb","stop");

      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
    }

    String query = "select * where not verb = 'stop'";
    JsonNode node = activities.query("select * where ordinal > 9");
    JsonNode incorrectNode = activities.query(query,"limit",Integer.toString(10));

    assertEquals(10, incorrectNode.get("entities").size());

    for(int i = 0 ; i < 10; i++)
      assertEquals(node.get("entities").get(i),incorrectNode.get("entities").get(i));
  }

  /*complete*/
  @Test //USERGRID-1520
  public void orderByComesBeforeLimitResult() {

    CustomCollection activities = collection("activities");

    long created = 0;
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();
    int checkResultsNum = 0;

    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");

    for (int i = 0; i < 20; i++) {
      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
      if (i == 0) { created = activity.findValue("created").getLongValue(); }
    }

    String query = "select * where created > " + created + " order by desc";
    String errorQuery =  query;

    JsonNode node = activities.query(query);
    JsonNode incorrectNode = activities.query(errorQuery,"limit",Integer.toString(5));

    assertEquals(5, incorrectNode.get("entities").size()); //asserts that limit works

    while(checkResultsNum < 5)
    {
      assertEquals(entityIndex(node,checkResultsNum),entityIndex(incorrectNode,checkResultsNum));
      //assertEquals(node.get("entities").get(checkResultsNum),incorrectNode.get("entities").get(checkResultsNum));
      checkResultsNum++;
    }
  }



  /*possibly complete ask for review*/
  @Test //USERGRID-1521
  public void orderByReturnCorrectResults(){

    CustomCollection activities = collection("activities");

    long created = 0;
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();

    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");

    for (int i = 0; i < 2000; i++) {
      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
      if (i == 1999) { created = activity.findValue("created").getLongValue(); }
    }

    String query = "select * where created >= " + Integer.toString(0);
    String errorQuery =  "select * where created <= " + created + " order by desc";

    JsonNode incorrectNode = activities.query(errorQuery);

    assertNotNull(incorrectNode.get("entities").get(0));
    //assertEquals(created,incorrectNode.get("entities").get(0).findValue("created").getLongValue());
    assertEquals(created,entityValue(incorrectNode,"created",0).getLongValue());


  }


  /*complete*/
  @Test //USERGRID-1615
  public void queryReturnCount() {

    CustomCollection activities = collection("activities");

    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();

    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");

    for (int i = 0; i < 20; i++) {
      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
    }

    String inCorrectQuery = "select * where verb = 'go' and ordinal >= 10 ";
    String correctQuery = "select * where ordinal >= 10";

    JsonNode incorrectNode = activities.query(inCorrectQuery);
    JsonNode correctNode =  activities.query(correctQuery);

    assertEquals(incorrectNode.get("entities").size(),correctNode.get("entities").size());

  }


  @Ignore("Loops Endlessly") //loops endlessly //Test to make sure all 1000 exist with a regular query
  public void queryReturnCheck() {
    CustomCollection madeupStuff = collection("imagination");
    Map character = hashMap("WhoHelpedYou","Ruff");

    for (int i = 0; i < 1000; i++) {
      character.put("Ordinal",i);
      madeupStuff.create(character);
    }

    String inquisitiveQuery = "select * where Ordinal >= 0 and Ordinal <= 2000 or WhoHelpedYou = 'Ruff'";
    JsonNode incorrectNode = madeupStuff.query(inquisitiveQuery);

    int totalEntitiesContained = 0;
    /* while (incorrectNode.get("entities").size() != 0)    for whatever reason, this test loops endlessly*/
    /* so does while (incorrectNode.get("entites") != null) works below it does not work here? why? */

    /*change out for method when get chance */
    while (incorrectNode.get("entities") != null)
    {
      totalEntitiesContained += incorrectNode.get("entities").size();
      incorrectNode = madeupStuff.query(inquisitiveQuery,"cursor",incorrectNode.get("cursor").toString());
    }
    assertEquals(1000,totalEntitiesContained);
  }

  @Ignore("Endlessly Loops")
  public void queryReturnCheckWithShortHand() {
    CustomCollection madeupStuff = collection("imagination");
    Map character = hashMap("WhoHelpedYou","Ruff");
    int[] ordinalOrder = new int[1001];

    for (int i = 0; i < 1001; i++) {
      character.put("Ordinal",i);
      ordinalOrder[i] = i;
      madeupStuff.create(character);
    }

    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff'";
    JsonNode incorrectNode = madeupStuff.query(inquisitiveQuery);

    int totalEntitiesContained = totalNumOfEntities(incorrectNode,inquisitiveQuery,madeupStuff,"");

    assertEquals(1001,totalEntitiesContained);

  }

  /*
  framework: make writing tests easier,
  for any given collection.
  run a standard set of tests.
  */





  @Test //Check to make sure that asc works
  public void queryCheckAsc() {
    /* can only do it in sets of 10's*/
    CustomCollection madeupStuff = collection("imagination");
    Map character = hashMap("WhoHelpedYou","Ruff");

    for (int i = 0; i < 1000; i++) {
      character.put("Ordinal",i);
      madeupStuff.create(character);
    }

    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff' ORDER BY " +
        "Ordinal asc";
    JsonNode incorrectNode = madeupStuff.query(inquisitiveQuery);

    //inquisitiveQuery = "select *";
    JsonNode node = madeupStuff.query("select *");

    int totalEntitiesContained = totalNumOfEntities(node,"select *",madeupStuff,inquisitiveQuery,incorrectNode);

    assertEquals(1000,totalEntitiesContained);
  }

  /*needs to be updated to reflect the fact that it also does validation*/
  public int totalNumOfEntities(JsonNode correctNode,String query,CustomCollection queryEndpoint,String checkedQuery,
                                JsonNode... checkedNodes) {

    int totalEntitiesContained = 0;
    while (correctNode.get("entities") != null)
    {
      totalEntitiesContained += correctNode.get("entities").size();
      if(checkedNodes.length !=0) {
        for(int index = 0; index < correctNode.get("entities").size();index++)
          assertEquals(correctNode.get("entities").get(index),checkedNodes[0].get("entities").get(index));

        if(checkedNodes[0].get("cursor") != null)
          checkedNodes[0] = queryEndpoint.query(checkedQuery,"cursor",checkedNodes[0].get("cursor").toString());
      }

      if(correctNode.get("cursor") != null)
        correctNode = queryEndpoint.query(query,"cursor",correctNode.get("cursor").toString());

      else
        break;
    }
    return totalEntitiesContained;
  }

  public JsonNode entityValue (JsonNode nodeSearched , String valueToSearch, int index) {
    return nodeSearched.get("entities").get(index).findValue(valueToSearch);
  }

  /*adds in key, with incrementing values starting at 0.*/
  public void createCollectionValues(CustomCollection collectionOfItems, Map valueHolder,int numOfValues,
                                     String... key) {

    for(int i = 0; i < numOfValues; i++) {
      for(int j = 0; j < key.length; j++) {
        valueHolder.put(key[j],i);
      }
      collectionOfItems.create(valueHolder);
    }
  }

   public JsonNode entityIndex(JsonNode container, int index) {
    return container.get("entities").get(index);
   }




  @Test
  public void restFrameWorkGetTest() {
    REST_Framework test_Framework_test = new REST_Framework();
    REST_Framework.testVariables testValueHolder= test_Framework_test.testReturn();
    CustomCollection newCollec = collection("imagination");

    testValueHolder.endpoint = "http://nobodyneedsthis.com";
    testValueHolder.requestType = "GET";
    testValueHolder.sql = "select * where hairstyle = 'bald'";
    testValueHolder.howMany = 2;
    testValueHolder.numValuesExpected = 1;

    test_Framework_test.mapValueAdder(testValueHolder,0,"WhoHelpedYou","Ruff");
    test_Framework_test.mapValueAdder(testValueHolder,0,"hairstyle","bald");
    test_Framework_test.mapValueAdder(testValueHolder,1,"WhoHelpedYou","BACONRUFFL");

    test_Framework_test.testCollectionsWith(testValueHolder,newCollec);
  }

  @Test
  public void restFrameWorkPutTest() {
    /* creates Framework instance*/
    REST_Framework test_Framework_test = new REST_Framework();
    REST_Framework.testVariables testValueHolder= test_Framework_test.testReturn();
    /*sets the name of the collection to be tested against*/
    CustomCollection newCollec = collection("imagination");

    /*populate values that the user wants to test for or with*/
    /*you wouldn't do that on each test */
    // testValueHolder.endpoint = "http://nobodyneedsthis.com";  /*configuration stuff*/
    testValueHolder.requestType = "Put"; /*http method <- change name to ; and enum*/
    testValueHolder.sql = "select * ";   /* general case you just need to make sure whateve ryou put in comes back*/
    testValueHolder.howMany = 2;     /* how many values you want to input, redundant;*/
    testValueHolder.numValuesExpected = 2; /* how many values you want to verify or expect from an entities.size
    comparison*/
    /*populate , then set of tests */

    /* mapValueAdder
    (specific instance of values to be tested against,
    index of hashmap that you want to add or modify,
    key of hash map,
    value of hash map)
     */
    test_Framework_test.mapValueAdder(testValueHolder,0,"WhoHelpedYou","Ruff");
    test_Framework_test.mapValueAdder(testValueHolder,0,"hairstyle","bald");
    test_Framework_test.mapValueAdder(testValueHolder,1,"WhoHelpedYou","BACONRUFFL");
    test_Framework_test.mapValueAdder(testValueHolder,0,"WhoHelpedYou","RUUUUFFFFFRRR");

    /*runs the test, creating above methods and running a test against the requestType and specified collection*/
    test_Framework_test.testCollectionsWith(testValueHolder,newCollec);
  }

  @Test//("Returned the error HTTP method DELETE doesn't support output")
  public void restFrameWorkDeleteTest() {
    REST_Framework test_Framework_test = new REST_Framework();
    REST_Framework.testVariables testValueHolder= test_Framework_test.testReturn();
    CustomCollection newCollec = collection("imagination");

    testValueHolder.endpoint = "http://nobodyneedsthis.com";
    testValueHolder.requestType = "Delete";
    testValueHolder.sql = "select * ";
    testValueHolder.howMany = 2;
    testValueHolder.numValuesExpected = 0;

    test_Framework_test.mapValueAdder(testValueHolder,0,"WhoHelpedYou","Ruff");
    test_Framework_test.mapValueAdder(testValueHolder,0,"hairstyle","bald");
    test_Framework_test.mapValueAdder(testValueHolder,1,"WhoHelpedYou","BACONRUFFL");

    test_Framework_test.testCollectionsWith(testValueHolder,newCollec);
  }


}



