package org.usergrid.rest.applications.collection.activities;

import org.codehaus.jackson.JsonNode;
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
public class PutTest extends RestContextTest{
  @Test //USERGRID-545
  public void  putMassUpdateTest () {

    CustomCollection activities = collection("activities");

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

    JsonNode node  = activities.withQuery(query).get();
    String uuid = node.get("entities").get(0).get("uuid").getTextValue();
    StringBuilder buf = new StringBuilder(uuid);


    activities.addToUrlEnd(buf);
    props.put("actor",newActor);
    node = activities.put(props);
    node = activities.withQuery(query).get();

    assertEquals(6,node.get("entities").size());
  }
}