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
package org.apache.usergrid.persistence.index.exceptions;


/**
 * Thrown when the user attempts to perform a "direct" operation with more than the max limit number of entities
 */
public class TooManyDirectEntitiesException extends QueryException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    final private int numberItemsRequested;
    final private int maxItemsAllowed;


    public TooManyDirectEntitiesException(int numberItemsRequested, int maxItemsAllowed) {
        super( "Exceeded maximum number of direct entities requested: "
                + Integer.toString(numberItemsRequested) + " requested, limit is " + Integer.toString(maxItemsAllowed));
        this.numberItemsRequested = numberItemsRequested;
        this.maxItemsAllowed = maxItemsAllowed;
    }


    public int getNumberItemsRequested() {
        return numberItemsRequested;
    }


    public int getMaxNumberItems() {
        return maxItemsAllowed;
    }
}
