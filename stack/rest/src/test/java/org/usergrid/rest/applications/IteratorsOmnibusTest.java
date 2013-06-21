package org.usergrid.rest.applications;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.CustomCollection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.usergrid.utils.MapUtils.hashMap;

import org.usergrid.rest.test.resource.ValueResource;


public class IteratorsOmnibusTest extends RestContextTest {

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
    //System.out.println(node.get("entities").)
    assertEquals(10, node.get("entities").size());

    query = query + " order by created desc";
    node = activities.query(query);
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

    String query = "select * where created > " + created + " order by desc";
    String errorQuery =  query;

    JsonNode node = activities.query(query);
    JsonNode incorrectNode = activities.query(errorQuery,"limit",5);

    assertEquals(5, incorrectNode.get("entities").size()); //asserts that limit works

    while(checkResultsNum < 5)
    {
      assertEquals(incorrectNode.get("entities").get(checkResultsNum),node.get("entities").get(checkResultsNum));
      checkResultsNum++;
    }
  }
}



