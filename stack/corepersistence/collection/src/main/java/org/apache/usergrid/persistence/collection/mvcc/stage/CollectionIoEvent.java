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
package org.apache.usergrid.persistence.collection.mvcc.stage;


import java.io.Serializable;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;


/**
 * @author tnine
 */
public class CollectionIoEvent<T> implements Serializable {

    private ApplicationScope context;

    private T event;


    public CollectionIoEvent( final ApplicationScope context, final T event ) {
        this.context = context;
        this.event = event;
    }


    public ApplicationScope getEntityCollection() {
        return context;
    }


    public T getEvent() {
        return event;
    }
}
