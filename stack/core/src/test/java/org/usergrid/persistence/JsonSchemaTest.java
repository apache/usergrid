package org.usergrid.persistence;

import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class JsonSchemaTest extends AbstractPersistenceTest {

    private static final Logger logger = LoggerFactory.getLogger(CollectionTest.class);

    @Test
    public void testCollection() throws Exception {
      UUID applicationId = createApplication("testOrganization",
              "testCollection");
      assertNotNull(applicationId);

      EntityManager em = emf.getEntityManager(applicationId);
      assertNotNull(em);

      Map<String, Object> properties = new LinkedHashMap<String, Object>();
      properties.put("name", "nico");

      Entity cat = em.create("cat", properties);
      assertNotNull(cat);
       
      JsonNode node = JsonUtils.parseToNode("{ \"type\":\"object\", \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"properties\":{ \"color\": { \"type\":\"string\" }, \"name\": { \"type\":\"string\" } }, \"required\" : [\"name\", \"color\"] }");
      
      em.setSchemaForEntityType("cat", node);
      
      properties = new LinkedHashMap<String, Object>();
      properties.put("name", "dylan");
      cat = em.create("cat", properties);
      assertNotNull(cat);
      
    }
}
