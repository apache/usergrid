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
package org.apache.usergrid.rest.test.resource;


import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;


/** @author tnine */
public abstract class SetResource extends ValueResource {

    public SetResource( String name, NamedResource parent ) {
        super( name, parent );
    }


    /** Get an entity resource by name */
    public EntityResource entity( String name ) {
        return new EntityResource( name, this );
    }


    /** Get an entity resource by Id */
    public EntityResource entity( UUID id ) {
        return new EntityResource( id, this );
    }


    /**
     * Get this resource with additional matrix parameters
     * @param params
     * @return
     */
    public MatrixResource withMatrix(Map<String, String> params){
        //once we want matrix params, we have to discard this current node in the chain, we need to construct
        //a special node to handle it
        return MatrixResource.build(this.getName(), params, this.getParent());
    }


    public int countEntities( String query ) {

        int totalEntitiesContained = 0;
        JsonNode correctNode =
                this.withQuery( query ).withLimit( 1000 ).get();//this.withQuery(query).get();//this.query
   
    /*change code to reflect the above */
        //this.withQuery().withCursor()
        while ( correctNode.get( "entities" ) != null ) {
            totalEntitiesContained += correctNode.get( "entities" ).size();
            if ( correctNode.get( "cursor" ) != null )
            //correctNode = this.query(query,"cursor",correctNode.get("cursor").toString());
            {
                correctNode = this.withQuery( query ).withCursor( correctNode.get( "cursor" ).toString() ).get();
            }
            else {
                break;
            }
        }
        return totalEntitiesContained;
    }

  /*cut out the key variable argument and move it into the customcollection call
  then just have it automatically add in the variable. */


    public JsonNode[] createEntitiesWithOrdinal( Map valueHolder, int numOfValues ) {

        JsonNode[] node = new JsonNode[numOfValues];

        for ( int i = 0; i < numOfValues; i++ ) {
            valueHolder.put( "Ordinal", i );
            node[i] = this.create( valueHolder );
        }

        return node;
    }
}
