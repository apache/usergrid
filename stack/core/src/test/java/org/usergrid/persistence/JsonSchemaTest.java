package org.usergrid.persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.exceptions.EntityValidationException;
import org.usergrid.persistence.exceptions.InvalidEntitySchemaSyntaxException;
import org.usergrid.utils.JsonUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class JsonSchemaTest extends AbstractPersistenceTest {

    private static final Logger logger = LoggerFactory
            .getLogger(CollectionTest.class);

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

        JsonNode bad_schema = readSchema("bad-schema.json");
        try {
            em.setSchemaForEntityType("cat", bad_schema);
            fail("Schema should have failed");
        } catch (InvalidEntitySchemaSyntaxException e) {
        }

        JsonNode cat_schema = readSchema("cat-schema.json");

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

    }

    JsonNode readSchema(String filename) throws JsonProcessingException,
            FileNotFoundException, IOException, URISyntaxException {
        return JacksonUtils.getReader().readTree(
                new FileReader(new File(JsonSchemaTest.class.getResource(
                        filename).toURI())));
    }
}
