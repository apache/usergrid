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


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;

import com.sun.jersey.api.client.WebResource;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public abstract class ValueResource extends NamedResource {

    private String name;
    private String query;
    private String cursor;
    private Integer limit;
    private UUID start;

    private Map<String, String> customParams;


    public ValueResource( String name, NamedResource parent ) {
        super( parent );
        this.name = name;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.rest.resource.NamedResource#addToUrl(java.lang.StringBuilder)
     */
    @Override
    public void addToUrl( StringBuilder buffer ) {
        parent.addToUrl( buffer );

        buffer.append( SLASH );

        buffer.append( name );
    }


    public void addToUrlEnd( StringBuilder buffer ) {
        buffer.append( SLASH );
        buffer.append( buffer );
    }


    /** Create a new entity with the specified data */
    public JsonNode create( Map<String, ?> entity ) {
        return postInternal( entity );
    }


    public JsonNode delete() {
        return deleteInternal();
    }


    /** post to the entity set */
    protected JsonNode postInternal( Map<String, ?> entity ) {

        return jsonMedia( withParams( withToken( resource() ) ) ).post( JsonNode.class, entity );
    }


    /** post to the entity set */
    protected JsonNode postInternal( Map<String, ?>[] entity ) {

        return jsonMedia( withParams( withToken( resource() ) ) ).post( JsonNode.class, entity );
    }


    public JsonNode put( Map<String, ?> entity ) {

        return putInternal( entity );
    }


    /** put to the entity set */
    protected JsonNode putInternal( Map<String, ?> entity ) {

        return jsonMedia( withParams( withToken( resource() ) ) ).put( JsonNode.class, entity );
    }


    /** Get the data */
    public JsonNode get() {
        return getInternal();
    }



    @SuppressWarnings("unchecked")
    public <T extends ValueResource> T withCursor( String cursor ) {
        this.cursor = cursor;
        return ( T ) this;
    }


    @SuppressWarnings("unchecked")
    public <T extends ValueResource> T withQuery( String query ) {
        this.query = query;
        return ( T ) this;
    }


    @SuppressWarnings("unchecked")
    public <T extends ValueResource> T withStart( UUID start ) {
        this.start = start;
        return ( T ) this;
    }


    @SuppressWarnings("unchecked")
    public <T extends ValueResource> T withLimit( Integer limit ) {
        this.limit = limit;
        return ( T ) this;
    }


    /** Query this resource. */

    @SuppressWarnings("unchecked")
    public <T extends ValueResource> T withParam( String name, String value ) {
        if ( customParams == null ) {
            customParams = new HashMap<String, String>();
        }

        customParams.put( name, value );

        return ( T ) this;
    }


    @Override
    protected WebResource withParams( final WebResource resource ) {
        WebResource withParams = super.withParams( resource );

        if(customParams == null){
            return withParams;
        }

        for(Entry<String, String> param : customParams.entrySet()){
            withParams = withParams.queryParam( param.getKey(), param.getValue());
        }

        return withParams;
    }


    /** Get entities in this collection. Cursor is optional */
    protected JsonNode getInternal() {


        WebResource resource = withParams( withToken( resource() ) );


        if ( query != null ) {
            resource = resource.queryParam( "ql", query );
        }

        if ( cursor != null ) {
            resource = resource.queryParam( "cursor", cursor );
        }

        if ( start != null ) {
            resource = resource.queryParam( "start", start.toString() );
        }

        if ( limit != null ) {
            resource = resource.queryParam( "limit", limit.toString() );
        }

        return jsonMedia( resource ).get( JsonNode.class );
    }


    public JsonNode query( String query, String addition, String numAddition ) {
        return getInternal( query, addition, numAddition );
    }


    protected JsonNode getInternal( String query, String addition, String numAddition ) {
        WebResource resource = withParams( withToken( resource() ) ).queryParam( "ql", query );

        if ( addition != null ) {
            resource = resource.queryParam( addition, numAddition );
        }


        if ( customParams != null ) {
            for ( Entry<String, String> param : customParams.entrySet() ) {
                resource = resource.queryParam( param.getKey(), param.getValue() );
            }
        }

        return jsonMedia( resource ).get( JsonNode.class );
    }


    public int verificationOfQueryResults( JsonNode[] correctValues, boolean reverse, String checkedQuery )
            throws Exception {

        int totalEntitiesContained = 0;

        JsonNode checkedNodes = this.withQuery( checkedQuery ).withLimit( 1000 ).get();

        while ( correctValues.length != totalEntitiesContained )//correctNode.get("entities") != null)
        {
            totalEntitiesContained += checkedNodes.get( "entities" ).size();
            if ( !reverse ) {
                for ( int index = 0; index < checkedNodes.get( "entities" ).size(); index++ ) {
                    assertEquals( correctValues[index].get( "entities" ).get( 0 ),
                            checkedNodes.get( "entities" ).get( index ) );
                }
            }
            else {
                for ( int index = 0; index < checkedNodes.get( "entities" ).size(); index++ ) {
                    assertEquals( correctValues[correctValues.length - 1 - index].get( "entities" ).get( 0 ),
                            checkedNodes.get( "entities" ).get( index ) );
                }
            }


      /*works because this method checks to make sure both queries return the same thing
      therefore this if shouldn't be needed, but added just in case
       */
            if ( checkedNodes.get( "cursor" ) != null ) {
                checkedNodes = this.query( checkedQuery, "cursor", checkedNodes.get( "cursor" ).toString() );
            }

            else {
                break;
            }
        }
        return totalEntitiesContained;
    }


    public JsonNode entityValue( String query, String valueToSearch, int index ) {
        JsonNode node = this.withQuery( query ).get();
        return node.get( "entities" ).get( index ).findValue( valueToSearch );
    }


    public JsonNode entityIndex( String query, int index ) {

        JsonNode node = this.withQuery( query ).get();
        return node.get( "entities" ).get( index );
    }


    public JsonNode entityIndexLimit( String query, Integer limitSize, int index ) {

        JsonNode node = this.withQuery( query ).withLimit( limitSize ).get();
        return node.get( "entities" ).get( index );
    }


    /** Get entities in this collection. Cursor is optional */
    protected JsonNode deleteInternal() {


        WebResource resource = withParams( withToken( resource() ) );

        if ( query != null ) {
            resource = resource.queryParam( "ql", query );
        }

        if ( cursor != null ) {
            resource = resource.queryParam( "cursor", cursor );
        }

        if ( start != null ) {
            resource = resource.queryParam( "start", start.toString() );
        }

        if ( limit != null ) {
            resource = resource.queryParam( "limit", limit.toString() );
        }

        return jsonMedia( resource ).delete( JsonNode.class );
    }
}
