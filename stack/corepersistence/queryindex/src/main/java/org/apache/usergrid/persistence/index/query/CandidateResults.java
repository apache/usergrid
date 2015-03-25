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
package org.apache.usergrid.persistence.index.query;

import java.util.Iterator;
import java.util.List;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Internal results class, should not be returned as results to a user.
 * Only returns candidate entity results
 */
public class CandidateResults implements Iterable<CandidateResult> {

    private static final Logger log = LoggerFactory.getLogger(CandidateResults.class);

    private String cursor = null;


    private final List<CandidateResult> candidates;


    public CandidateResults( List<CandidateResult> candidates ) {
        this.candidates = candidates;
    }

    public void add( List<CandidateResult> candidates ) {
        this.candidates.addAll( candidates);
    }

    public void initializeCursor(){
        //USERGRID-461 our cursor is getting too large, map it to a new time UUID
        //TODO T.N., this shouldn't live here. This should live at the UG core tier.  However the RM/EM are an absolute mess, so until they're refactored, this is it's home

        cursor = org.apache.usergrid.persistence.index.utils.StringUtils.sanitizeUUID(UUIDGenerator.newTimeUUID());
    }


    public boolean hasCursor() {
        return cursor != null;
    }


    public String getCursor() {
        return cursor;
    }


    public void setCursor(String cursor) {
        this.cursor = cursor;
    }




    public int size() {
        return candidates.size();
    }


    public boolean isEmpty() {
        return candidates.isEmpty();
    }


    /**
     * Get the candidates
     * @return
     */
    public CandidateResult get(int index){
        return candidates.get( index );
    }

    @Override
    public Iterator<CandidateResult> iterator() {
        return candidates.iterator();
    }

}
