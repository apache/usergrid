/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.pipeline.read;


import com.google.common.base.Optional;


/**
 * A bean that is passed between filters with immutable cursor state
 * @param <T>
 */
public class FilterResult<T> {
    private final T value;
    private final Optional<EdgePath> path;


    /**
     * Create a new immutable filtervalue
     * @param value The value the filter emits
     * @param path The path to this value, if created
     */
    public FilterResult( final T value, final Optional<EdgePath> path ) {
        this.value = value;
        this.path = path;
    }


    public T getValue() {
        return value;
    }


    public Optional<EdgePath> getPath() {
        return path;
    }


}
