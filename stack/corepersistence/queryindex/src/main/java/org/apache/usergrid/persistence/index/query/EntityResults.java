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
package org.apache.usergrid.persistence.index.query;


import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * Loads results from candidate results.  This needs to be refactored to the calling module, and should not exist in the
 * query index
 */
public class EntityResults implements Iterable<Entity>, Iterator<Entity> {


    private final CandidateResults results;
    private final EntityCollectionManager ecm;
    private final UUID maxVersion;
    private final Iterator<CandidateResult> itr;
    private Entity next = null;


    public EntityResults( final CandidateResults results, final EntityCollectionManager ecm, final UUID maxVersion ) {
        this.results = results;
        this.ecm = ecm;
        this.maxVersion = maxVersion;
        this.itr = results.iterator();
    }


    @Override
    public Iterator<Entity> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
       if(next == null){
           doAdvance();
       }

       return next != null;
    }


    /**
     * Advance to our next candidate so that it is avaiablel
     */
    private void doAdvance(){
        while(itr.hasNext() && next == null){
            CandidateResult candidate = itr.next();

            //our candidate is > our max, we can't use it
            if( UUIDUtils.compare( candidate.getVersion(), maxVersion ) > 0){
                continue;
            }

            //our candidate was too new, ignore it
            next = ecm.load( candidate.getId() ).toBlockingObservable().single();
        }
    }


    @Override
    public Entity next() {
        if(!hasNext()){
            throw new NoSuchElementException("No more elements in the iterator");
        }


        Entity result =  next;

        next = null;

        return result;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is not supported" );
    }


}
