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
package org.apache.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.cassandra.CursorCache;


/** Simple iterator that just returns UUIDs that are set into it */
public class StaticIdIterator implements ResultIterator {

    private final Set<ScanColumn> ids;

    private boolean returnedOnce = false;


    /**
     *
     */
    public StaticIdIterator( UUID id ) {
        final ScanColumn col = new UUIDColumn( id );

        ids = Collections.singleton( col );
    }


    @Override
    public void reset() {
        //no op
    }


    @Override
    public void finalizeCursor( CursorCache cache, UUID lastValue ) {
        //no cursor, it's a static list
    }


    @Override
    public Iterator<Set<ScanColumn>> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        return !returnedOnce;
    }


    @Override
    public Set<ScanColumn> next() {
        returnedOnce = true;
        return ids;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "This iterator does not support remove" );
    }
}
