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

package org.apache.usergrid.persistence.collection.mvcc.changelog;/*
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


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.usergrid.persistence.model.field.Field;


/**
 * Create a new instance of the change log
 */
public class ChangeLogImpl implements ChangeLog {

    private final HashMap<String, Field> additions;
    private final HashSet<String> deletions;


    /**
     * @param approximateMaxFieldCount The estimated upper bounds of the changelog, helps with efficiency by
     * overallocation if necessary
     */
    public ChangeLogImpl( final int approximateMaxFieldCount ) {
        //todo, may be way too large
        additions = new HashMap( approximateMaxFieldCount );
        deletions = new HashSet( approximateMaxFieldCount );
    }


    /**
     * Add change logs to this log
     *
     * @param deletes The set containing all string fields to delete
     */
    public void addDeletes( final Set<String> deletes ) {
        this.deletions.addAll( deletes );
        clearAdditions( deletes );
    }


    /**
     * Add the field as a write column
     */
    public void addWrite( final Field field ) {
        final String fieldName = field.getName();
        this.additions.put( field.getName(), field );
        this.deletions.remove( fieldName );
    }


    /**
     * Remove the names from the additions and deletions
     */
    public void clear( final Set<String> names ) {
        this.deletions.removeAll( names );
        clearAdditions( names );
    }


    /**
     * Clear additions by name
     */
    private void clearAdditions( final Set<String> deletes ) {

        for ( final String key : deletes ) {
            this.additions.remove( key );
        }
    }


    /**
     * Clear all all additions and deletions
     */
    public void clear() {
        this.additions.clear();
        this.deletions.clear();
    }


    @Override
    public Set<String> getDeletes() {
        return deletions;
    }


    @Override
    public Collection<Field> getWrites() {
        return additions.values();
    }


    @Override
    public int getSize() {
        return deletions.size() + additions.size();
    }
}
