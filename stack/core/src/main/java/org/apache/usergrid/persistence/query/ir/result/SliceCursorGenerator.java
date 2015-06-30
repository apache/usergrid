/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;

import org.apache.usergrid.persistence.cassandra.CursorCache;
import org.apache.usergrid.persistence.query.ir.QuerySlice;


/**
 * A cursor generator for the specified query slice
 */
public class SliceCursorGenerator implements CursorGenerator<ScanColumn> {

    private final QuerySlice slice;


    public SliceCursorGenerator( final QuerySlice slice ) {this.slice = slice;}


    @Override
    public void addToCursor( final CursorCache cache, final ScanColumn col ) {

        if ( col == null ) {
            return;
        }

        final int sliceHash = slice.hashCode();


        ByteBuffer bytes = col.getCursorValue();


        cache.setNextCursor( sliceHash, bytes );
    }
}
