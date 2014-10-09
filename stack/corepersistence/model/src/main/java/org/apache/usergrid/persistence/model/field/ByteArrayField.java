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


/**
 * A field for storing and array of bytes.
 */
public class ByteArrayField extends AbstractField<byte[]> {

    Class classinfo;

    public ByteArrayField( String name, byte[] value,Class classinfo ) {
        super( name, value );
        this.classinfo = classinfo;
    }

    public ByteArrayField() {

    }

    @Override
    public byte[] getValue() {
        return value;
    }

    public Class getClassinfo() {
        return classinfo;
    }


    @Override
    public FieldTypeName getTypeName() {
        return FieldTypeName.BYTE_ARRAY;
    }
}
