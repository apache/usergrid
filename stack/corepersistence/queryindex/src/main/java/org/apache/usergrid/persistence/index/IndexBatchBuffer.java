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
package org.apache.usergrid.persistence.index;

import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;

/**
 * Buffer for index operations,
 */
public interface IndexBatchBuffer {

    /**
     * flush out buffer and execute
     */
    public void flush();

    /**
     * flush out buffer and execute
     */
    public void flushAndRefresh();

    /**
     * put request into buffer
     * @param builder
     */
    public void put(IndexRequestBuilder builder);

    /**
     * put request into buffer
     * @param builder
     */
    public void put(DeleteRequestBuilder builder);

}
