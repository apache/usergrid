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
package org.apache.usergrid.rest.test.resource;


import java.util.Map;


/**
 * Class for returning elements of a collection via matrix
 */
public class MatrixResource extends SetResource {


    private MatrixResource( final String name, final NamedResource parent ) {
        super( name, parent );
    }


    /**
     * build the matrix resource.  We have to do this due to java super limitations
     */
    public static MatrixResource build( final String name, final Map<String, String> params,
                                        final NamedResource parent ) {
        StringBuilder builder = new StringBuilder();

        builder.append( name );

        for ( Map.Entry<String, String> entry : params.entrySet() ) {
            builder.append( ";" );
            builder.append( entry.getKey() );
            builder.append( "=" );
            builder.append( entry.getValue() );
        }


        return new MatrixResource( builder.toString(), parent );
    }

    public CollectionResource collection(String name) {
        return new CollectionResource( name, this );
    }


    public Connection connection(String name){
        return new Connection(name, this);
    }
}
