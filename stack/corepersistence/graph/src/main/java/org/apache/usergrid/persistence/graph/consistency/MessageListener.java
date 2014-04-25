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
package org.apache.usergrid.persistence.graph.consistency;


import org.apache.usergrid.persistence.collection.OrganizationScope;

import rx.Observable;


/**
 *
 *
 */
public interface MessageListener<T, R> {


    /**
     * The handler to receive the event.  Any exception that is thrown is considered
     * a failure, and the event will be re-fired.
     * @param event  The input event
     * @return The observable that performs the operations
     */
    Observable<R> receive(final T event);

}
