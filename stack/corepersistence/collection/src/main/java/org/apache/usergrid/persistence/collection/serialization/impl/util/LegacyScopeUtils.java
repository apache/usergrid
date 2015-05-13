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

package org.apache.usergrid.persistence.collection.serialization.impl.util;


/**
 * This class is a helper to keep our legacy CollectionScope for older serialization apis.  This will generate
 * the scope in the same way previous scopes were generated in the core module.    This only exists to migrate
 * perviously collection scoped Api's to versions without them.
 * This can be removed once we do an official release and prune our impls.
 */
public class LegacyScopeUtils {

    /**
       * Edge types for collection suffix
       */
      public static final String EDGE_COLL_SUFFIX = "zzzcollzzz";



    /**
      * Get the collection name from the entity/id type
      * @param type
      * @return
      */
     public static String getCollectionScopeNameFromEntityType( String type ) {
         String csn = EDGE_COLL_SUFFIX +  Inflector.INSTANCE.pluralize( type );
         return csn.toLowerCase();
     }




}
