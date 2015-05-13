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
package org.apache.usergrid.persistence.collection.mvcc.entity;


import org.apache.usergrid.persistence.collection.MvccEntity;

import com.google.common.base.Preconditions;

import static org.apache.usergrid.persistence.core.util.ValidationUtils.verifyEntityWrite;
import static org.apache.usergrid.persistence.core.util.ValidationUtils.verifyIdentity;
import static org.apache.usergrid.persistence.core.util.ValidationUtils.verifyTimeUuid;
import static org.apache.usergrid.persistence.core.util.ValidationUtils.verifyVersion;


/**
 * Validation Utilities for collection
 */
public class MvccValidationUtils {


    /**
     * Verify the version is correct.  Also verifies the contained entity
     */
    public static void verifyMvccEntityWithEntity( MvccEntity entity ) {


        Preconditions.checkNotNull( entity.getEntity().isPresent(), "Entity is required" );
        verifyVersion( entity.getVersion() );
        verifyMvccEntityOptionalEntity( entity );
    }

    /**
        * Verify the version is correct.  Does not verify the contained entity of it is null
        */
       public static void verifyMvccEntityOptionalEntity( MvccEntity entity ) {


           verifyIdentity( entity.getId() );

           verifyTimeUuid( entity.getVersion(), "version" );

           if ( entity.getEntity().isPresent() ) {
               verifyEntityWrite( entity.getEntity().orNull() );
           }
       }





}
