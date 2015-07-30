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
package org.apache.usergrid.services.assets.data;


import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.apache.usergrid.persistence.Entity;


public interface BinaryStore {

    /**
     * writes the inputStream to the store and updates the entity's file-metadata field. however, it doesn't persistent
     * the entity.
     */
    void write( UUID appId, Entity entity, InputStream inputStream ) throws Exception;

    /** read the entity's file data from the store */
    InputStream read( UUID appId, Entity entity ) throws Exception;

    /** read partial data from the store */
    InputStream read( UUID appId, Entity entity, long offset, long length ) throws Exception;

    /** delete the entity data from the store. */
    void delete( UUID appId, Entity entity ) throws Exception;
}
