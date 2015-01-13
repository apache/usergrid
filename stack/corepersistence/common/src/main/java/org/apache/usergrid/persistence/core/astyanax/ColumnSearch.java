/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.core.astyanax;


import com.netflix.astyanax.util.RangeBuilder;


/**
 *
 *
 */
public interface ColumnSearch<T> {

    /**
     * Set the start value supplied and the user supplied end value (if present)
     *
     * @param value The value to set in the start
     */
    public void buildRange( final RangeBuilder rangeBuilder, final T value );

    /**
     * Set the range builder with the user supplied start and finish
     */
    public void buildRange( final RangeBuilder rangeBuilder );

    /**
     * Return true if we should skip the first result
     * @return
     */
    public boolean skipFirst(final T first);
}
