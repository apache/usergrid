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

package org.apache.usergrid.persistence.index;

import java.util.List;
import java.util.UUID;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Search results are Id and Version references to Entities.
 */
public class EntitySearchResults {
    private final long count;
    private final List<Ref> refs;

    public EntitySearchResults( long count, List<Ref> refs ) {
        this.count = count;
        this.refs = refs;
    }

    public long count() {
        return count;
    }

    public List<Ref> getRefs() {
        return refs;
    }

    /**
     * Reference to one specific version of an Entity.
     */
    public static final class Ref {
        private Id id;
        private UUID version;

        public Ref( Id id, UUID version ) {
            this.id = id;
            this.version = version;
        }
        public Id getId() {
            return id;
        }
        public UUID getVersion() {
            return version;
        }
    }
}
