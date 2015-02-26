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


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static org.junit.Assert.assertEquals;
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


    private void assertUserWithBlocks( org.apache.usergrid.persistence.model.entity.Entity e ) {

        assertTrue( e.getField("block") instanceof ListField );
        assertTrue( e.getField("block").getValue() instanceof List );
        List blockList = (List)e.getField("block").getValue();

        EntityObject entityObject = (EntityObject)blockList.get(0);
        assertEquals("fred", entityObject.getField("name").getValue());
    }

}
