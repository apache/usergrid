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
    JsonNode node = groups.withQuery(query).get();
    assertEquals(1,node.get("entities").size());

    assertEquals(index[3],node.get("entities").get(0).get("created").getLongValue());
  }
  @Test // USERGRID-1401
  public void groupQueriesWithConsistentResults() {

    CustomCollection groups = context.application().collection("groups");

    int maxRangeLimit = 20;
    JsonNode[] saved  = new JsonNode[maxRangeLimit];
    
    Map<String, String> actor = hashMap("displayName", "Erin");
    Map<String, Object> props = new HashMap<String, Object>();

    props.put("actor",actor);
    Map<String, Integer> location = hashMap("latitude",37);
    location.put("longitude",-75);
    props.put ("location",location);
    props.put("verb", "go");
    props.put("content", "bragh");
  
    for (int i = 0; i < 20; i++) {
      String newPath = String.format("/kero" + i);
      props.put("path",newPath);
      props.put("ordinal", i);
      JsonNode activity = groups.create(props).get("entities").get(0);
      saved[i] = activity;
    }

    JsonNode node = null;
    for(int consistent = 0; consistent < 20; consistent++) {
      String query = String.format("select * where location within 100 of 37, -75 and ordinal >= %d and ordinal < %d" ,  saved[7].get("ordinal").asLong(),  saved[10].get("ordinal").asLong());

      node = groups.withQuery(query).get(); //groups.query(query);
      
      JsonNode entities = node.get("entities");
      
      assertEquals(3, entities.size());
      
      for(int i = 0; i < 3; i++) {
        //shouldn't start at 10 since you're excluding it above in the query, it should return 9,8,7
        assertEquals(saved[7+i], entities.get(i));
      }
    }
  }
}