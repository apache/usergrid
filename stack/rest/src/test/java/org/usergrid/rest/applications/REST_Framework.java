package org.usergrid.rest.applications;

import org.junit.Rule;
import org.junit.rules.Verifier;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org. junit.runners.Suite;
import static org.junit.experimental.results.PrintableResult.testResult;
import static org.junit.experimental.results.ResultMatchers.isSuccessful;

import org.apache.commons.lang.ArrayUtils;

import org.codehaus.jackson.JsonNode;
import static org.junit.Assert.assertThat;
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
import java.lang.Object;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */

/*
The below is how you would run a set of tests on code you have written below
@RunWith(Suite.class)
@Suite.SuiteClasses(IteratorsOmnibusTest.class)
*/


public class REST_Framework extends RestContextTest{


  private static Map[] cloneVerify;
  private static int entitySize;

  /*writing tests easier! make it easier than what you have
  parse the json
  I know an endpoint should be able to do a bigger suite of tests
  */

//  public static class entitySetVerify {
//
//
//    @Rule
//    public Verifier collector = new Verifier() {
//      @Override
//      protected void verify() {
//      /* so this needs to get back the entities that we are pushing in and compare them to see
//      if they are equal, the only way i can think of doing this is to create clones of the values
//      then running them?
//       */
//
//        assertEquals(39393,109);
//      }
//    };
//  }

  @Test    //things yet to implement: if you have more than 10 entities then you'll have to page through them.
  public String testCollectionsWith(testVariables input, CustomCollection testCollection) {

    if (input.requestType.toLowerCase().equals("get") ||
        input.requestType.toLowerCase().equals("post")){

      for(int i = 0; i < input.howMany; i++) {
        testCollection.create(input.valuesForRequestType[i]);
      }
      JsonNode node = null;//testCollection.query(input.sql); /* not sure if a post could be handled this way */
      //TODO: make this handle calls to limit or cursor queries
      entitySize = node.get("entities").size();
      //assertThat(testResult(entitySetVerify.class),isSuccessful());

      assertEquals(input.numValuesExpected,node.get("entities").size());

    }

    if (input.requestType.toLowerCase().equals("delete")) {


//      for(int i = 0; i < input.howMany; i++) {
//        testCollection.create(input.valuesForRequestType[i]);
//      }
//
//
//      for(int i = 0; i < input.howMany; i++) {
//        testCollection.delete(input.valuesForRequestType[i]);
//      }
//      JsonNode node = testCollection.query(input.sql);
//      //JsonNode node = testCollection.delete(input.valuesForRequestType);
//      assertEquals(input.numValuesExpected,node.get("entities").size());
//
//    }
//    if (input.requestType.toLowerCase().equals("put")) {
//
//      //testCollection.create(input.valuesForRequestType);
//
//      for(int i = 0; i < input.howMany; i++) {
//        testCollection.put(input.valuesForRequestType[i]);
//      }
//    //  JsonNode node = testCollection.put(input.valuesForRequestType);
//      JsonNode node = testCollection.query(input.sql);
//      assertEquals(input.numValuesExpected,node.get("entities").size()); /*need to be
//      fed information here because some might be updates or new values. */
    }

    return "passed";
  }


  public static class testVariables{
    String endpoint;
    String requestType;
    String sql;
    int howMany;
    Map[] valuesForRequestType;
    int numValuesExpected;

    /*public void setEndpoint (String endPointGiven) {
      this.endpoint = endPointGiven;
    } */

    //public void
      /* make this into a hash map instead in order to have people put values in */
    /* maybe on that has values they want to initialize with and then another with values they want to have
    included in the request type.        */
    /* have more values that would represent what they want to get back form the test case
    for example: how many results did they expect back? another ArrayOfSomething that would reflect how many
     */
    /* what kind of verification could I do? maybe a size() check...check to make sure each ordinal got in?
    What if they don't give me an ordinal, would it be fine to add it in?
     */
  }
  /* not that readable :< add it to the test value holder? */
  /*create a test , accept the inputs, */
  public void mapValueAdder(testVariables input,int mapIndex, Object key, Object value) { //HashMap valueInserted) {

    if(input.valuesForRequestType == null) {
      input.valuesForRequestType =  new Map[input.howMany];

    }
    if(input.valuesForRequestType[mapIndex] == null) {
      input.valuesForRequestType[mapIndex] = hashMap(key,value);
    }
    else {
      input.valuesForRequestType[mapIndex].put(key,value);
    }
    cloneVerify = input.valuesForRequestType.clone();
  }

  public testVariables testReturn () {
    return new testVariables();
  }
}
