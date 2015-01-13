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

package org.apache.usergrid.persistence.core.astyanax;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * A utility class for storing multiple fields
 */
public class FieldBuffer {

    private final List<ByteBuffer> fields;


    /**
     * Allocate a new field buffer with the expected max size.  This allows us to pre-allocate
     * our buffer for fields
     *
     * @param expectedMax
     */
    public FieldBuffer(final int expectedMax){
        fields = new ArrayList<>( expectedMax );
    }


    /**
     * Add the field and the serializer to the list
     * @param value   The serialized value to add to the buffer
     */
    public void add(ByteBuffer value){
        //Note that we're not validating our byte buffer length.  Since the length of a byte buffer IS an integer
        //we can't possibly overflow an integer because they're the same data type
        fields.add(value.duplicate() );
    }


    /**
     * Return the list of all fields in read only format
     * @return
     */
    public List<ByteBuffer> getFields(){
        return Collections.unmodifiableList(fields);
    }




}
