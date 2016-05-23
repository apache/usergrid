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
package org.apache.usergrid.java.client.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsergridResponseError {

    @Nullable private String errorName;
    @Nullable private String errorDescription;
    @Nullable private String errorException;

    @NotNull private final Map<String, JsonNode> properties = new HashMap<>();

    public UsergridResponseError() {
        this(null,null,null);
    }
    public UsergridResponseError(@Nullable final String errorName) {
        this(errorName, null, null);
    }
    public UsergridResponseError(@Nullable final String errorName, @Nullable final String errorDescription) {
        this(errorName,errorDescription,null);
    }
    public UsergridResponseError(@Nullable final String errorName, @Nullable final String errorDescription, @Nullable final String errorException) {
        this.errorName = errorName;
        this.errorDescription = errorDescription;
        this.errorException = errorException;
    }

    @NotNull
    @JsonAnyGetter
    public Map<String, JsonNode> getProperties() {
        return properties;
    }

    @JsonAnySetter
    public void setProperty(@NotNull final String key, @NotNull final JsonNode value) {
        properties.put(key, value);
    }

    @Nullable
    @JsonProperty("error")
    public String getErrorName() {
        return errorName;
    }

    @JsonProperty("error")
    public void setErrorName(@NotNull final String errorName) {
        this.errorName = errorName;
    }

    @Nullable
    @JsonProperty("exception")
    public String getErrorException() {
        return errorException;
    }

    @JsonProperty("exception")
    public void setErrorException(@NotNull final String errorException) {
        this.errorException = errorException;
    }

    @Nullable
    @JsonProperty("error_description")
    public String getErrorDescription() {
        return errorDescription;
    }

    @JsonProperty("error_description")
    public void setErrorDescription(@NotNull final String errorDescription) {
        this.errorDescription = errorDescription;
    }
}
