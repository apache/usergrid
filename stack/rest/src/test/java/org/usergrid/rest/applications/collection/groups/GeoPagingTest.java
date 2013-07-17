package org.usergrid.rest.applications.collection.groups;

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
public class GeoPagingTest extends RestContextTest {
  @Test //("Test uses up to many resources to run reliably") // USERGRID-1403
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
    for (int i = 0; i < 5; i++) {
      String newPath = String.format("/kero" + i);
      props.put("path",newPath);
      props.put("ordinal", i);
      JsonNode activity = groups.create(props);
      index[i] = activity.findValue("created").getLongValue();
    }

    String query = "select * where location within 20000 of 37,-75 and created > " + index[2] + " and " +
        "created < " + index[4] + "";
    JsonNode node = groups.withQuery(query).get();//groups.query(query);
    assertEquals(1,node.get("entities").size());

    assertEquals(index[3],node.get("entities").get(0).get("created").getLongValue());
  }
  @Test//("Test uses up to many resources to be run reliably") // USERGRID-1401
  public void groupQueriesWithConsistentResults() {

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
    for(int consistent = 0; consistent < 20; consistent++) {
      String query = "select * where location within 20000 of 37,-75 and created >= " + index[7] + " and " +
          "created < " + index[10];

      node = groups.withQuery(query).get(); //groups.query(query);
      assertEquals(3,node.get("entities").size());
      for(int i = 3; i > 0; i++) {
        assertNotNull(node.get("entities").get(i).get("created").getLongValue());
        assertEquals(index[10-i],node.get("entities").get(i).get("created").getLongValue());
      }
    }
  }
}