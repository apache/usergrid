/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest.applications.utils;


import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.junit.Ignore;
import org.apache.usergrid.utils.UUIDUtils;


/** @author tnine */
@Ignore("Not a test")
public class TestUtils {

    /** Get the uuid at the given index for the root node.  If it doesn't exist, null is returned */
    public static UUID getIdFromSearchResults( JsonNode rootNode, int index ) {
        JsonNode entityArray = rootNode.get( "entities" );

        if ( entityArray == null ) {
            return null;
        }

        JsonNode entity = entityArray.get( index );

        if ( entity == null ) {
            return null;
        }

        return UUIDUtils.tryExtractUUID( entity.get( "uuid" ).asText() );
    }

    /** Get the uuid at the given index for the root node.  If it doesn't exist, null is returned */
    public static UUID getIdFromSearchResults( Collection collection, int index ) {


        if ( collection == null ) {
            return null;
        }

        Entity entity = (Entity)collection.getResponse().getEntities().get(index);

        if ( entity == null ) {
            return null;
        }

        return UUIDUtils.tryExtractUUID( entity.get( "uuid" ).toString() );
    }
}
