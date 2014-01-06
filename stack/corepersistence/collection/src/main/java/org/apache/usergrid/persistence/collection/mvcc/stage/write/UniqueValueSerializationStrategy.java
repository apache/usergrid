/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.mvcc.stage.write;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.List;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.model.entity.Id;

/**
 * Reads and writes to UniqueValues column family.
 */
public interface UniqueValueSerializationStrategy {

    public MutationBatch write( CollectionScope context, UniqueValue entity, String fieldName );

    public UniqueValue load( CollectionScope colScope, 
            Id entityId, final UUID version, String fieldName ) throws ConnectionException;

    public List<UniqueValue> load( CollectionScope context, 
            Id entityId, String fieldName ) throws ConnectionException;

    public MutationBatch delete( CollectionScope context, Id entityId, String fieldName );
}
