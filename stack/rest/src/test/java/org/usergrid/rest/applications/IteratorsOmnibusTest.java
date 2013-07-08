package org.usergrid.rest.applications;

import org.apache.commons.lang.ArrayUtils;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.CustomCollection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.usergrid.utils.MapUtils.hashMap;





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


    JsonNode node = activities.query(query,"limit",Integer.toString(10));

    int totalEntitiesContained = 0;
    while (node.get("entities") != null)
    {
      totalEntitiesContained += node.get("entities").size();

      if(node.get("cursor") != null)
        node = activities.query(query,"cursor",node.get("cursor").toString());
      else
        break;
    }
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
  /*
  @Test // USERGRID-1401
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

    for(int consistant = 0; consistant < 20; consistant++) {
      String query = "select * where location within 20000 of 37,-75 and created >= " + index[7] + " and " +
      "created < " + index[10];

      node = groups.query(query);
      assertEquals(3,node.get("entities").size());
    }



    for(int i = 2; i>0; i++)
      assertEquals(index[10-i],node.get("entities").get(i).get("created").getLongValue());
}

  */
  /*completed*/
  /*
  @Test // USERGRID-1403
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
    */
  /*complete*/
  @Test //USERGRID-1475
  public void orderByDisplayFullQueriesInLimit() {

    CustomCollection activities = collection("activities");

    long created = 0;
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
      assertEquals(node.get("entities").get(checkResultsNum),incorrectNode.get("entities").get(checkResultsNum));
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
    assertEquals(created,incorrectNode.get("entities").get(0).findValue("created").getLongValue());

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

    JsonNode incorrectNode = activities.query(inCorrectQuery,"limit",Integer.toString(10));
    JsonNode correctNode =  activities.query(correctQuery);

    assertEquals(incorrectNode.get("entities").size(),correctNode.get("entities").size());


  }
  /* includes test case with endless cursor looping for a strange reason */
//
//  @Test //Test to make sure all 1000 exist with a regular query
//  public void queryReturnCheck() {
//    CustomCollection madeupStuff = collection("imagination");
//    Map character = hashMap("WhoHelpedYou","Ruff");
//
//    for (int i = 0; i < 1000; i++) {
//      character.put("Ordinal",i);
//      madeupStuff.create(character);
//    }
//
//    String inquisitiveQuery = "select * where Ordinal >= 0 and Ordinal <= 2000 or WhoHelpedYou = 'Ruff'";
//    JsonNode incorrectNode = madeupStuff.query(inquisitiveQuery);
//
//    int totalEntitiesContained = 0;
//    /* while (incorrectNode.get("entities").size() != 0)    for whatever reason, this test loops endlessly*/
//    /* so does while (incorrectNode.get("entites") != null) works below it does not work here? why? */
//
//    while (incorrectNode.get("entities") != null)
//    {
//      totalEntitiesContained += incorrectNode.get("entities").size();
//
//      incorrectNode = madeupStuff.query(inquisitiveQuery,"cursor",incorrectNode.get("cursor").toString());
//
//
//    }
//
//    assertEquals(1000,totalEntitiesContained);
//
//  }
//
//  @Test //Checks to make sure short hand works
//  public void queryReturnCheckWithShortHand() {
//    CustomCollection madeupStuff = collection("imagination");
//    Map character = hashMap("WhoHelpedYou","Ruff");
//    int[] ordinalOrder = new int[1001];
//
//    for (int i = 0; i < 1001; i++) {
//      character.put("Ordinal",i);
//      ordinalOrder[i] = i;
//      madeupStuff.create(character);
//    }
//
//    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff'";
//    JsonNode incorrectNode = madeupStuff.query(inquisitiveQuery);
//
//    int totalEntitiesContained = 0;
//    while (incorrectNode.get("entities") != null)
//    {
//      totalEntitiesContained += incorrectNode.get("entities").size();
//
//      if(incorrectNode.get("cursor") != null)
//        incorrectNode = madeupStuff.query(inquisitiveQuery,"cursor",incorrectNode.get("cursor").toString());
//      else
//        break;
//    }
//
//    assertEquals(1001,totalEntitiesContained);
//
//  }

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

    int totalEntitiesContained = 0;
    while (incorrectNode.get("entities") != null)
    {
      int incrementedBy;

      if(incorrectNode.get("entities").size() != 0)     {
        totalEntitiesContained += incorrectNode.get("entities").size();
        incrementedBy = incorrectNode.get("entities").size();
      }
      else {
        assertNull(incorrectNode.get("cursor"));
        break;

      }

      int entityCheck = 0;
      for(int index = totalEntitiesContained-incrementedBy; index < totalEntitiesContained; index++) {

        assertEquals(index,incorrectNode.get("entities").get(entityCheck).get("Ordinal").asInt());
        entityCheck++;

      }

      if(incorrectNode.get("cursor").toString() != null) {

        incorrectNode = madeupStuff.query(inquisitiveQuery,"cursor",incorrectNode.get("cursor").toString());
      }
      else
        break;
    }

    assertEquals(1000,totalEntitiesContained);

  }


}



