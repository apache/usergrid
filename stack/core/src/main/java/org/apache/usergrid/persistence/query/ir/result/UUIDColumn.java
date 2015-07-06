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
import java.util.UUID;

import org.apache.usergrid.persistence.cassandra.Serializers;
import org.apache.usergrid.utils.UUIDUtils;


/**
 * Used as a comparator for columns
 */
class UUIDColumn extends AbstractScanColumn{

    private final int compareReversed;


    public UUIDColumn( final UUID uuid, final int compareReversed, final CursorGenerator<UUIDColumn> sliceCursorGenerator   ) {
        super(uuid, Serializers.ue.toByteBuffer( uuid ), sliceCursorGenerator );
        this.compareReversed = compareReversed;
    }




    @Override
    public int compareTo( final ScanColumn other ) {
        return  UUIDUtils.compare( uuid, other.getUUID() ) * compareReversed;
    }


}
