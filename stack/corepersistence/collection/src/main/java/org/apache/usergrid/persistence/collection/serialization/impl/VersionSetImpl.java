/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.persistence.collection.serialization.impl;/*
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


import java.util.HashMap;
import java.util.Map;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Implementation of the version set
 */
public class VersionSetImpl implements VersionSet {

    private final Map<Id, MvccLogEntry> logEntries;

    public VersionSetImpl(final int maxSize){
        logEntries = new HashMap<>( maxSize );
    }

    public void addEntry(final MvccLogEntry entry){
        this.logEntries.put( entry.getEntityId(), entry );
    }

    @Override
    public MvccLogEntry getMaxVersion( final Id entityId ) {
        return this.logEntries.get( entityId );
    }


    @Override
    public int size() {
        return this.logEntries.size();
    }


    @Override
    public boolean isEmpty() {
        return this.logEntries.isEmpty();
    }
}
