/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.stack;


import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;


public class CoordinatedClusterDeserializer extends JsonDeserializer<ICoordinatedCluster> {

    @Override
    public ICoordinatedCluster deserialize( final JsonParser jp, final DeserializationContext ctxt )
            throws IOException {

        String tmp = jp.getText();
        validate( jp, tmp, "{" );

        jp.nextToken();
        tmp = jp.getText();

        BasicCluster delegate = new BasicCluster();
        CoordinatedCluster cluster = new CoordinatedCluster( delegate );

        while( jp.hasCurrentToken() ) {

            tmp = jp.getText();

            if ( tmp.equals( CoordinatedClusterSerializer.NAME ) ) {
                jp.nextToken();
                delegate.setName( jp.getText() );
            }
            else if ( tmp.equals( CoordinatedClusterSerializer.SIZE ) ) {
                jp.nextToken();
                delegate.setSize( jp.getIntValue() );
            }
            else if ( tmp.equals( CoordinatedClusterSerializer.INSTANCE_SPEC ) ) {
                jp.nextToken();
                delegate.setInstanceSpec( jp.readValuesAs( InstanceSpec.class ).next() );
            }
            else if ( tmp.equals( CoordinatedClusterSerializer.INSTANCES ) ) {
                jp.nextToken();
                jp.nextToken();
                Iterator<Instance> iterator = jp.readValuesAs( Instance.class );
                while ( iterator.hasNext() ) {
                    cluster.add( iterator.next() );
                }
            }

            jp.nextToken();

            if ( jp.getText().equals( "}" ) ) {
                break;
            }
        }

        return cluster;
    }


    private void validate( JsonParser jsonParser, String input, String expected ) throws JsonProcessingException {
        if ( ! input.equals( expected ) ) {
            throw new JsonParseException( "Unexpected token: " + input, jsonParser.getTokenLocation() );
        }
    }
}
