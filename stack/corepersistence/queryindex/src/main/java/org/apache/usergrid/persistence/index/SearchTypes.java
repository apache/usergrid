package org.apache.usergrid.persistence.index;/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */


import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.impl.IndexingUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import java.util.Arrays;


/**
 * Class to encapsulate search types
 */

public class SearchTypes {

    private static final SearchTypes ALL_TYPES = new SearchTypes(  );

    private final String[] types;


    private SearchTypes( final String... types ) {this.types = types;}


    public String[] getTypeNames(ApplicationScope applicationScope) {
        String[] typeNames = new String[types.length*2];
        int i =0 ;
        for(String type : types){
            typeNames[i++] = IndexingUtils.getType(applicationScope,type);
            typeNames[i++] = type;
        }
        return typeNames;
    }



    /**
     * Create a search that will search on the specified types
     * @param types
     * @return
     */
    public static SearchTypes fromTypes( final String... types ) {
        return new SearchTypes( types );
    }


    /**
     * Get a search that will search all types in the specified context
     * @return
     */
    public static SearchTypes allTypes(){
        return ALL_TYPES;
    }


    /**
     * Create a search type from a potentially nullable set of string.  If they are null, or empty, then allTypes is returned
     * otherwise the type will be returned
     * @param types
     * @return
     */
    public static SearchTypes fromNullableTypes(final String... types){

        if(isEmpty(types) ){
            return allTypes();
        }

        return fromTypes( types );
    }


    /**
     * Return true if the array is empty, or it's elements contain a null
     * @param input
     * @return
     */
    private static boolean isEmpty(final String[] input){
        if(input == null || input.length == 0){
            return true;
        }

        for(int i = 0; i < input.length; i ++){
            if(input[i] == null){
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof SearchTypes ) ) {
            return false;
        }

        final SearchTypes that = ( SearchTypes ) o;

        if ( !Arrays.equals( types, that.types ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        return Arrays.hashCode( types );
    }


    @Override
    public String toString() {
        return "SearchTypes{" +
                "types=" + Arrays.toString( types ) +
                '}';
    }
}
