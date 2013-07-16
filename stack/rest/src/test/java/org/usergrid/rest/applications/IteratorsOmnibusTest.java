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

//  @Test //Check to make sure that asc works
//  public void queryCheckAsc() {
//    /* can only do it in sets of 10's*/
//    CustomCollection madeupStuff = collection("imagination");
//    Map character = hashMap("WhoHelpedYou","Ruff");
//
//    madeupStuff.createEntitiesWithOrdinal(character,1000);
//
//    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff' ORDER BY " +
//        "Ordinal asc";
//    String query = "select *";
//
//    int totalEntitiesContained = madeupStuff.verificationOfQueryResults(query,inquisitiveQuery);
//
//    assertEquals(1000,totalEntitiesContained);
//  }

  public JsonNode entityValue (JsonNode nodeSearched , String valueToSearch, int index) {
    return nodeSearched.get("entities").get(index).findValue(valueToSearch);
  }

  public JsonNode entityIndex(JsonNode container, int index) {
    return container.get("entities").get(index);
  }

}


