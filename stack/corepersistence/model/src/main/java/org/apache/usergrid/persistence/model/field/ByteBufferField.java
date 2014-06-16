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
package org.apache.usergrid.persistence.model.field;

import java.nio.ByteBuffer;

import org.codehaus.jackson.annotate.JsonTypeInfo;


/**
 * A field for storing byte buffers
 */
public class ByteBufferField extends AbstractField<ByteBuffer> {

    Class classinfo;
    /**
     * Creates an immutable copy of the byte buffer
     */
    public ByteBufferField( String name, ByteBuffer value,Class classinfo ) {
        //always return a duplicate so we don't mess with the markers
        super( name, value.duplicate() );
        this.classinfo = classinfo;
    }

    public ByteBufferField() {

    }

    @Override
    public ByteBuffer getValue() {
        //always return a duplicate so we don't mess with the markers
        return value.duplicate();
    }

    public Class getClassinfo() {
        return classinfo;
    }
}
