/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.index.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.junit.Test;
import static org.junit.Assert.*;


public class EntityUtilsTest {
    
    /**
     * Test of mapToEntity method, of class EntityUtils.
     */
    @Test
    public void testMapToEntityRoundTrip() throws IOException {

        InputStream is = this.getClass().getResourceAsStream( "/sample.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> contacts = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        for ( Object o : contacts ) {

            Map<String, Object> map1 = (Map<String, Object>)o;

            // convert map to entity
            Entity entity1 = EntityUtils.mapToEntity( map1 );

            // convert entity back to map
            Map map2 = EntityUtils.entityToMap( entity1 );

            // the two maps should be the same
            Map diff = Maps.difference( map1, map2 ).entriesDiffering();
            assertTrue( Maps.difference( map1, map2 ).areEqual() );
        }
    }
}
