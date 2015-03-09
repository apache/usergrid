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
package org.apache.usergrid.mongo.commands;


import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.mongo.MongoChannelHandler;
import org.apache.usergrid.mongo.protocol.OpQuery;
import org.apache.usergrid.mongo.protocol.OpReply;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;

import static org.apache.usergrid.utils.MapUtils.entry;
import static org.apache.usergrid.utils.MapUtils.map;


public class Count extends MongoCommand {

    private static final Logger logger = LoggerFactory.getLogger( Count.class );


    @Override
    public OpReply execute( MongoChannelHandler handler, ChannelHandlerContext ctx, MessageEvent e, OpQuery opQuery ) {

        OpReply reply = new OpReply( opQuery );

        ApplicationInfo application = SubjectUtils.getApplication( Identifier.from( opQuery.getDatabaseName() ) );

        if ( application == null ) {
            return reply;
        }

        EntityManager em = handler.getEmf().getEntityManager( application.getId() );

        try {
            Results results =
                    em.getCollection( em.getApplicationRef(), ( String ) opQuery.getQuery().get( "count" ), null,
                            100000, Level.IDS, false );
            reply.addDocument( map( entry( "n", results.size() * 1.0 ), entry( "ok", 1.0 ) ) );
        }
        catch ( Exception ex ) {
            logger.error( "Unable to retrieve collections", ex );
        }
        return reply;
    }
}
