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

package org.apache.usergrid.corepersistence.command.cursor;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * A cursor used in rendering a response
 */
public class ResponseCursor {



    /**
     * We use a map b/c some indexes might be skipped
     */
    private Map<Integer, ? super Serializable> cursors = new HashMap<>();

    /**
     * Set the possible cursor value into the index. DOES NOT parse the cursor.  This is intentional for performance
     */
    public <T extends Serializable> void setCursor( final int id, final T cursor ) {
        cursors.put( id, cursor );
    }


    private void ensureCapacity() {

    }


    public String encodeAsString() {
        return null;
    }
}
