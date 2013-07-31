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
    }

    String errorQuery = "select * where created >= " + created + "AND madeup = true";
    JsonNode incorrectNode = activities.withQuery(errorQuery).get();

    assertEquals(10,incorrectNode.get("entities").size());

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
    JsonNode incorrectNode = activities.query(query,"limit",Integer.toString(10));

    assertEquals(10, incorrectNode.get("entities").size());

    for(int i = 0 ; i < 10; i++) {
      assertEquals(19-i,incorrectNode.get("entities").get(i).get("ordinal").getIntValue());
      assertEquals("stop",incorrectNode.get("entities").get(i).get("verb").getTextValue());
    }
  }

  @Test //USERGRID-1615
  public void queryReturnCount() throws Exception{

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
  }

  @Test //Check to make sure that asc works
  public void queryCheckAsc() throws Exception{

    CustomCollection madeupStuff = collection("imagination");
    Map character = hashMap("WhoHelpedYou","Ruff");

    madeupStuff.createEntitiesWithOrdinal(character,1000);

    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff' ORDER BY " +
        "Ordinal asc";
    String query = "select *";

    int totalEntitiesContained = madeupStuff.verificationOfQueryResults(query,inquisitiveQuery);

    assertEquals(1000,totalEntitiesContained);
  }

  @Ignore//Test to make sure all 1000 exist with a regular query
  public void queryReturnCheck() throws Exception{
    CustomCollection madeupStuff = collection("imagination");
    Map character = hashMap("WhoHelpedYou","Ruff");

    madeupStuff.createEntitiesWithOrdinal(character,1000);

    String query = "select *";
    String inquisitiveQuery = "select * where Ordinal >= 0 and Ordinal <= 2000 or WhoHelpedYou = 'Ruff'";

    int totalEntitiesContained = madeupStuff.verificationOfQueryResults(query,inquisitiveQuery);

    assertEquals(1000,totalEntitiesContained);
  }

  @Ignore
  public void queryReturnCheckWithShortHand() {
    CustomCollection madeupStuff = collection("imagination");
    Map character = hashMap("WhoHelpedYou","Ruff");

    madeupStuff.createEntitiesWithOrdinal(character,1000);

    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff'";

    int totalEntitiesContained = madeupStuff.countEntities(inquisitiveQuery);

    assertEquals(1000,totalEntitiesContained);

  }

}