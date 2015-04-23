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

import com.google.common.base.Optional;


/**
 * A cursor that has been passed in with our request.  Adds utils for parsing values
 */
public class RequestCursor {

    public RequestCursor(final Optional<String> cursor){

    }


    /**
     * Get the cursor with the specified id
     */
    public <T extends Serializable> T getCursor( final int id, final Class<T> cursorType ) {
        return null;
    }

}
