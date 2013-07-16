package org.usergrid.rest.applications.collection.activities;

import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.CustomCollection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class AndOrQueryTest extends RestContextTest {
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

    JsonNode node = activities.withQuery(query).get();
    JsonNode incorrectNode = activities.withQuery(errorQuery).get();

    assertEquals(node.get("entities").size(),incorrectNode.get("entities").size());

  }

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

    String query = "select * where not verb = 'go'";
    JsonNode node = activities.withQuery("select * where ordinal > 9").get();
    JsonNode incorrectNode = activities.query(query,"limit",Integer.toString(10));

    assertEquals(10, incorrectNode.get("entities").size());

    for(int i = 0 ; i < 10; i++)
      assertEquals(node.get("entities").get(i),incorrectNode.get("entities").get(i));
  }

  @Test //USERGRID-1615
  public void queryReturnCount() {

    CustomCollection activities = collection("activities");

    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();

    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");

    activities.createEntitiesWithOrdinal(props,20);

    String inCorrectQuery = "select * where verb = 'go' and ordinal >= 10 ";
    String correctQuery = "select * where ordinal >= 10";

    activities.verificationOfQueryResults(correctQuery,inCorrectQuery);

    //assertEquals(activities.countEntities(inCorrectQuery),activities.countEntities(correctQuery));

    // assertEquals(activities.countEntities(correctQuery),activities.countEntities(inCorrectQuery));

  }

  @Test //Check to make sure that asc works
  public void queryCheckAsc() {
    /* can only do it in sets of 10's*/
    CustomCollection madeupStuff = collection("imagination");
    Map character = hashMap("WhoHelpedYou","Ruff");

    madeupStuff.createEntitiesWithOrdinal(character,1000);

    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff' ORDER BY " +
        "Ordinal asc";
    String query = "select *";

    int totalEntitiesContained = madeupStuff.verificationOfQueryResults(query,inquisitiveQuery);

    assertEquals(1000,totalEntitiesContained);
  }

  @Test //@Ignore("Loops Endlessly") //loops endlessly //Test to make sure all 1000 exist with a regular query
  public void queryReturnCheck() {
    CustomCollection madeupStuff = collection("imagination");
    Map character = hashMap("WhoHelpedYou","Ruff");

    madeupStuff.createEntitiesWithOrdinal(character,1000);

    String query = "select *";
    String inquisitiveQuery = "select * where Ordinal >= 0 and Ordinal <= 2000 or WhoHelpedYou = 'Ruff'";

    int totalEntitiesContained = madeupStuff.verificationOfQueryResults(query,inquisitiveQuery);

    assertEquals(1000,totalEntitiesContained);
  }

  @Ignore("Endlessly Loops")
  public void queryReturnCheckWithShortHand() {
    CustomCollection madeupStuff = collection("imagination");
    Map character = hashMap("WhoHelpedYou","Ruff");

    madeupStuff.createEntitiesWithOrdinal(character,1001);

    for (int i = 0; i < 1001; i++) {
      character.put("Ordinal",i);
      madeupStuff.create(character);
    }

    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff'";
    //JsonNode incorrectNode = madeupStuff.query(inquisitiveQuery);

    int totalEntitiesContained = madeupStuff.countEntities(inquisitiveQuery); //totalNumOfEntities(incorrectNode,
    // inquisitiveQuery,madeupStuff,
    // "");

    assertEquals(1001,totalEntitiesContained);

  }

}