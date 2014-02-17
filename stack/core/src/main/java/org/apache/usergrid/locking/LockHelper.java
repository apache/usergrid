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
package org.apache.usergrid.locking;


import java.util.UUID;

import org.apache.usergrid.locking.exception.UGLockException;
import org.apache.usergrid.locking.noop.NoOpLockImpl;

import org.apache.commons.codec.binary.Hex;

import static org.apache.usergrid.locking.LockPathBuilder.buildPath;
import static org.apache.usergrid.utils.ConversionUtils.bytes;


/** @author tnine */
public class LockHelper {


    private static final NoOpLockImpl NO_OP_LOCK = new NoOpLockImpl();


    /**
     * Build a string path for this lock.  Since it's specifically for updating a property, the property needs appended
     * to the path.  If the property is null, it's getting deleted, so a lock on it isn't neccessary.  In that case, a
     * no op lock is returned
     */
    public static Lock getUniqueUpdateLock( LockManager manager, UUID applicationId, Object value, String... path )
            throws UGLockException {
        //we have no value, therefore there's nothing to lock
        if ( value == null ) {
            return NO_OP_LOCK;
        }

        return manager.createLock( applicationId, buildPath( Hex.encodeHexString( bytes( value ) ), path ) );
    }
}
