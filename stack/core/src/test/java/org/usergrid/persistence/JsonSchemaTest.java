package org.usergrid.persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.usergrid.utils.JsonUtils.loadJsonFromResourceFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.exceptions.EntityValidationException;
import org.usergrid.persistence.exceptions.InvalidEntitySchemaSyntaxException;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonSchemaTest extends AbstractPersistenceTest {

  private static final Logger logger = LoggerFactory
      .getLogger(CollectionTest.class);

  @Test
  public void testCollection() throws Exception {
    UUID applicationId = createApplication("testOrganization", "testCollection");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put("name", "nico");

    Entity cat = em.create("cat", properties);
    assertNotNull(cat);

    JsonNode bad_schema = loadJsonFromResourceFile(JsonSchemaTest.class,
        JsonNode.class, "bad-schema.json");
    assertNotNull(bad_schema);
    try {
      em.setSchemaForEntityType("cat", bad_schema);
      fail("Schema should have failed");
    } catch (InvalidEntitySchemaSyntaxException e) {
    }

    JsonNode cat_schema = loadJsonFromResourceFile(JsonSchemaTest.class,
        JsonNode.class, "cat-schema.json");
    assertNotNull(cat_schema);

    em.setSchemaForEntityType("cat", cat_schema);

    properties = new LinkedHashMap<String, Object>();
    properties.put("name", "dylan");

    try {
      cat = em.create("cat", properties);
      fail("Entity cat should have failed");
    } catch (EntityValidationException e) {
    }

    properties.put("color", "black");
    cat = em.create("cat", properties);
    assertNotNull(cat);

    try {
      em.setProperty(cat, "color", "purple");
      fail("Setting property color to purple should have failed");
    } catch (EntityValidationException e) {
    }

    em.setProperty(cat, "color", "orange");
    
    try {
      em.setProperty(cat, "foo", "bar");
      fail("Setting property foo to bar should have failed");
    } catch (EntityValidationException e) {
    }
  }

}
