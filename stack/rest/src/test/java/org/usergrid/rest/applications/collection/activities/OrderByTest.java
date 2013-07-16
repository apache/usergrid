package org.usergrid.rest.applications.collection.activities;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.CustomCollection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class OrderByTest extends RestContextTest {

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
    JsonNode node = activities.withQuery(query).get();
    assertEquals(10, node.get("entities").size());

    query = query + " order by created desc";
    node = activities.withQuery(query).get();
    assertEquals(10, node.get("entities").size());
  }

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

    String query = "select * where created > " + 1 + " order by created desc";
    String errorQuery =  query;

    JsonNode node = activities.withQuery(query).get();//activities.query(query);
    JsonNode incorrectNode = activities.withQuery(errorQuery).withLimit("5").get();

    assertEquals(5, incorrectNode.get("entities").size()); //asserts that limit works

    while(checkResultsNum < 5)
    {
      assertEquals(activities.entityIndex(query,checkResultsNum),
          activities.entityIndexLimit(errorQuery,"5",checkResultsNum));
      //assertEquals(node.get("entities").get(checkResultsNum),incorrectNode.get("entities").get(checkResultsNum));
      checkResultsNum++;
    }
  }
  /*public JsonNode entityIndex(JsonNode container, int index) {
    return container.get("entities").get(index);
  } */

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

    JsonNode incorrectNode = activities.withQuery(errorQuery).get();//activities.query(errorQuery);

    assertNotNull(incorrectNode.get("entities").get(0));
    //assertEquals(created,incorrectNode.get("entities").get(0).findValue("created").getLongValue());
    assertEquals(created,(activities.entityValue(errorQuery,"created",0)).getLongValue());
  }
}
