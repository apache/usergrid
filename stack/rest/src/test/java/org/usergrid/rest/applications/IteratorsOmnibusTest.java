package org.usergrid.rest.applications;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.CustomCollection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.usergrid.utils.MapUtils.hashMap;

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
    assertEquals(10, node.get("entities").size());

    query = query + " order by created desc";
    node = activities.query(query);
    assertEquals(10, node.get("entities").size());
  }
}
