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
package com.usergrid.count.common;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
 * @author zznate
 */
public class CountSerDeUtils {

    public static String serialize(Count count) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(count);
        } catch (Exception ex) {
            throw new CountTransportSerDeException("Problem in serialize() call",ex);
        }
    }

    public static Count deserialize(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(JsonMethod.CREATOR, JsonAutoDetect.Visibility.ANY);

        try {
            return mapper.readValue(json, Count.class);
        } catch (IOException e) {
            throw new CountTransportSerDeException("Problem in deserialize() call", e);
        }
    }
}
