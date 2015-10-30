/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import junit.framework.Assert;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.value.EntityObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class CpEntityMapUtilsTest {
    private static final Logger log = LoggerFactory.getLogger( CpEntityMapUtilsTest.class );

    @Test
    public void testToMap() {

        Map<String, Object> properties = new LinkedHashMap<String, Object>() {{
            put( "username", "bart" );
            put( "email", "bart@example.com" );
            put( "block", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "fred"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "gertrude"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "mina"); }});
            }});
            put( "blockedBy", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "isabell"); }});
            }});
            put( "location", new LinkedHashMap<String, Object>() {{
                put("latitude", 37.776753 );
                put("longitude", -122.407846 );
            }});
        }};

        Entity cpEntity = CpEntityMapUtils.fromMap( properties, "user", true );
        assertUserWithBlocks( cpEntity );
    }

    @Test
    public void testNestedArrayToMap() {

        /*** This tests example property input of

             {
                "nestedarray" : [ [ "fred" ] ]
             }

         ****/

        Map<String, Object> properties = new LinkedHashMap<String, Object>() {{
            put( "nestedarray",
                new ArrayList<ArrayList<String>>() {{
                    add(0, new ArrayList<String>() {{
                        add(0, "fred");
                        }});
                }}
            );
            put( "block", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "fred"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "gertrude"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "mina"); }});
            }});
        }};

        Entity cpEntity = CpEntityMapUtils.fromMap( properties, "user", true );
        assertUserWithBlocks(cpEntity);
        Map<String,Object> map = CpEntityMapUtils.toMap(cpEntity);
        cpEntity = CpEntityMapUtils.fromMap(map,"user", true);
    }


    @Test
    public void testLocation() {

        /*** This tests example property input of

         {
         "nestedarray" : [ [ "fred" ] ]
         }

         ****/
        Map<String, Object> locMap = new HashMap<>();
        locMap.put("latitude", 123.1);
        locMap.put("longitude", 123.1);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("location", locMap);

        Entity cpEntity = CpEntityMapUtils.fromMap(properties, "loc", true);

        assertTrue(cpEntity.getFieldMap().get("location") instanceof LocationField);


        locMap = new HashMap<>();
        locMap.put("latitude", 123.1);
        locMap.put("lgnosoos", 123.1);

        properties = new LinkedHashMap<String, Object>();
        properties.put("location", locMap);

        cpEntity = CpEntityMapUtils.fromMap(properties, "loc", true);

        assertTrue(cpEntity.getFieldMap().get("location") instanceof EntityObjectField);

        properties = new LinkedHashMap<String, Object>();
        properties.put("location", "denver");

        cpEntity = CpEntityMapUtils.fromMap(properties, "loc", true);

        assertTrue(cpEntity.getFieldMap().get("location") instanceof StringField);
    }



    @Test
    public void testSerialization() throws JsonProcessingException, IOException {

        Map<String, Object> properties = new LinkedHashMap<String, Object>() {{
            put( "username", "bart" );
            put( "email", "bart@example.com" );
            put( "block", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "fred"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "gertrude"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "mina"); }});
            }});
            put( "blockedBy", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "isabell"); }});
            }});
            put( "location", new LinkedHashMap<String, Object>() {{
                put("latitude", 37.776753 );
                put("longitude", -122.407846 );
            }});
        }};

        org.apache.usergrid.persistence.model.entity.Entity entity =
            new org.apache.usergrid.persistence.model.entity.Entity(
                new SimpleId( "user" ) );
        entity = CpEntityMapUtils.fromMap( entity, properties, null, true );

        assertUserWithBlocks( entity );

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class");

        String entityString = mapper.writeValueAsString( entity );
        //log.debug("Serialized to JSON: " + entityString );

        TypeReference<Entity> tr = new TypeReference<Entity>() {};
        entity = mapper.readValue( entityString, tr );
        //log.debug("Round-tripped entity: " + CpEntityMapUtils.toMap(entity) );

        assertUserWithBlocks( entity );
    }


    @Test
    public void testNestedArraySerialization() throws JsonProcessingException, IOException {

        /*** This tests example property input of

         {
         "nestedarray" : [ [ "fred" ] ]
         }

         ****/

        Map<String, Object> properties = new LinkedHashMap<String, Object>() {{
            put( "nestedarray",
                new ArrayList<ArrayList<String>>() {{
                    add(0, new ArrayList<String>() {{
                        add(0, "fred");
                    }});
                }}
            );
            put( "block", new ArrayList<Object>() {{
                add( new LinkedHashMap<String, Object>() {{ put("name", "fred"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "gertrude"); }});
                add( new LinkedHashMap<String, Object>() {{ put("name", "mina"); }});
            }});
        }};

        org.apache.usergrid.persistence.model.entity.Entity entity =
            new org.apache.usergrid.persistence.model.entity.Entity(
                new SimpleId( "user" ) );
        entity = CpEntityMapUtils.fromMap( entity, properties, null, true );

        assertUserWithBlocks( entity );

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class");

        String entityString = mapper.writeValueAsString( entity );
        //log.debug("Serialized to JSON: " + entityString );

        TypeReference<Entity> tr = new TypeReference<Entity>() {};
        entity = mapper.readValue( entityString, tr );
        //log.debug("Round-tripped entity: " + CpEntityMapUtils.toMap(entity) );

        assertUserWithBlocks( entity );
    }


    private void assertUserWithBlocks( org.apache.usergrid.persistence.model.entity.Entity e ) {

        assertTrue( e.getField("block") instanceof ListField );
        assertTrue( e.getField("block").getValue() instanceof List );
        List blockList = (List)e.getField("block").getValue();

        EntityObject entityObject = (EntityObject)blockList.get(0);
        assertEquals("fred", entityObject.getField("name").getValue());
    }

}
