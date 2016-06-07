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
package org.apache.usergrid.java.client.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class UsergridEntityDeserializer extends JsonDeserializer<UsergridEntity> {

    @NotNull private static final ObjectMapper objectMapper = new ObjectMapper();

    @NotNull
    public UsergridEntity deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        UsergridEntity entity = UsergridEntityDeserializer.objectMapper.readValue(jsonParser,UsergridEntity.class);
        Class<? extends UsergridEntity> entitySubClass = UsergridEntity.customSubclassForType(entity.getType());
        if( entitySubClass != null ) {
            entity = JsonUtils.mapper.convertValue(entity,entitySubClass);
        }
        return entity;
    }
}
