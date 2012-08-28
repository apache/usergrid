/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest.applications.utils;

import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.usergrid.utils.UUIDUtils;

/**
 * @author tnine
 *
 */
@Ignore("Not a test")
public class TestUtils {

    /**
     * Get the uuid at the given index for the root node.  If it doesn't exist, null is returned 
     * @param rootNode
     * @param index
     * @return
     */
    public static UUID getIdFromSearchResults(JsonNode rootNode, int index) {
        JsonNode entityArray = rootNode.get("entities");
        
        if(entityArray == null){
            return null;
        }
        
        JsonNode entity = entityArray.get(index);
        
        if(entity == null){
            return null;
        }
        
        return UUIDUtils.tryExtractUUID(entity
                .get("uuid").asText());

    }

}
