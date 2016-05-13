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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.*;
import org.apache.usergrid.java.client.exception.UsergridException;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("unused")
public final class JsonUtils {

    @NotNull public static final ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(UsergridEntity.class, new UsergridEntityDeserializer());
        mapper.registerModule(module);
    }

    @NotNull
    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

    @Nullable
    public static String getStringProperty(@NotNull final Map<String, JsonNode> properties, @NotNull final String name) {
        JsonNode value = properties.get(name);
        if (value != null) {
            return value.asText();
        }
        return null;
    }

    @NotNull
    public static ArrayList<Object> convertToArrayList(@NotNull final ArrayNode arrayNode) {
        ArrayList<Object> arrayList = new ArrayList<>();
        Iterator<JsonNode> iterator = arrayNode.elements();
        while( iterator.hasNext() ) {
            arrayList.add(iterator.next());
        }
        return arrayList;
    }

    @NotNull
    public static String toJsonString(@NotNull final Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonGenerationException e) {
            throw new UsergridException("Unable to generate json", e);
        } catch (JsonMappingException e) {
            throw new UsergridException("Unable to map json", e);
        } catch (IOException e) {
            throw new UsergridException("IO error", e);
        }
    }

    @NotNull
    public static String toPrettyJsonString(@NotNull final Object obj) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonGenerationException e) {
            throw new UsergridException("Unable to generate json", e);
        } catch (JsonMappingException e) {
            throw new UsergridException("Unable to map json", e);
        } catch (IOException e) {
            throw new UsergridException("IO error", e);
        }
    }

    @NotNull
    public static JsonNode toJsonNode(@NotNull final Object obj) {
        return mapper.convertValue(obj, JsonNode.class);
    }

    @NotNull
    public static Map toMap(@NotNull final Object obj) {
        return mapper.convertValue(obj,Map.class);
    }

    @NotNull
    public static <T> T fromJsonNode(@NotNull final JsonNode json, @NotNull final Class<T> c) {
        try {
            JsonParser jp = json.traverse();
            return mapper.readValue(jp, c);
        } catch (JsonGenerationException e) {
            throw new UsergridException("Unable to generate json", e);
        } catch (JsonMappingException e) {
            throw new UsergridException("Unable to map json", e);
        } catch (IOException e) {
            throw new UsergridException("IO error", e);
        }
    }

    public static void setObjectProperty(@NotNull final Map<String, JsonNode> properties, @NotNull final String name, @Nullable final ObjectNode value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getProperty(@NotNull final Map<String, JsonNode> properties, @NotNull final String name) {
        JsonNode value = properties.get(name);
        if( value == null ) {
            return null;
        } else if (value instanceof TextNode) {
            return (T) value.asText();
        } else if (value instanceof LongNode) {
            Long valueLong = value.asLong();
            return (T) valueLong;
        } else if (value instanceof BooleanNode) {
            Boolean valueBoolean = value.asBoolean();
            return (T) valueBoolean;
        } else if (value instanceof IntNode) {
            Integer valueInteger = value.asInt();
            return (T) valueInteger;
        } else if (value instanceof FloatNode) {
            return (T) Float.valueOf(value.toString());
        } else {
            return (T) value;
        }
    }
}
