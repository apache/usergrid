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


import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import com.google.common.base.Optional;



/**
 * Internal results class, should not be returned as results to a user.
 * Only returns candidate entity results
 */
public class CandidateResults implements Iterable<CandidateResult> {

    private Optional<Integer> offset = null;


    private final List<CandidateResult> candidates;
    private final Collection<SelectFieldMapping> getFieldMappings;

    public CandidateResults( List<CandidateResult> candidates, final Collection<SelectFieldMapping> getFieldMappings) {
        this.candidates = candidates;
        this.getFieldMappings = getFieldMappings;
        offset = Optional.absent();
    }


    public void initializeOffset( int offset ){
        this.offset = Optional.of(offset);
    }


    public boolean hasOffset() {
        return offset.isPresent();
    }


    public Optional<Integer> getOffset() {
        return offset;
    }


    public void setOffset(int offset) {
        this.offset = Optional.of(offset);
    }




    public int size() {
        return candidates.size();
    }


    public boolean isEmpty() {
        return candidates.isEmpty();
    }


    public Collection<SelectFieldMapping> getGetFieldMappings() {
        return getFieldMappings;
    }


    /**
     * Get the candidates
     * @return
     */
    public CandidateResult get(int index){
        return candidates.get(index);
    }

    @Override
    public Iterator<CandidateResult> iterator() {
        return candidates.iterator();
    }

}
