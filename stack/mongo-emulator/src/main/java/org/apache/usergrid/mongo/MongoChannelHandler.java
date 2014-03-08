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
package org.apache.usergrid.mongo;


import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.mongo.protocol.Message;
import org.apache.usergrid.mongo.protocol.OpCrud;
import org.apache.usergrid.mongo.protocol.OpReply;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.services.ServiceManagerFactory;

import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;


public class MongoChannelHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger( MongoChannelHandler.class );

    private final EntityManagerFactory emf;
    private final ServiceManagerFactory smf;
    private final ManagementService management;
    private final SessionsSecurityManager securityManager;

    Subject subject = null;


    public MongoChannelHandler( EntityManagerFactory emf, ServiceManagerFactory smf, ManagementService management,
                                SessionsSecurityManager securityManager ) {
        super();

        logger.info( "Starting new client connection..." );
        this.emf = emf;
        this.smf = smf;
        this.management = management;
        this.securityManager = securityManager;

        if ( securityManager != null ) {
            subject = new Subject.Builder( securityManager ).buildSubject();
        }
    }


    public EntityManagerFactory getEmf() {
        return emf;
    }


    public ServiceManagerFactory getSmf() {
        return smf;
    }


    public ManagementService getOrganizations() {
        return management;
    }


    public SessionsSecurityManager getSecurityManager() {
        return securityManager;
    }


    @Override
    public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) {

        ThreadState threadState = null;
        if ( subject != null ) {
            threadState = new SubjectThreadState( subject );
            threadState.bind();
            // logger.info("Security subject bound to thread");
        }

        try {

            Message message = null;
            if ( e.getMessage() instanceof Message ) {
                message = ( Message ) e.getMessage();
            }

            if ( message != null ) {
                logger.info( ">>> {}\n", message );
                OpReply reply = handleMessage( ctx, e, message );
                logger.info( "<<< {}\n", reply );

                if ( reply != null ) {
                    e.getChannel().write( reply );
                }
            }
        }
        //an error occurred.  Set this as the attachment so subsequent calls to getlastError can return correct
        //errors
        catch ( Exception ex ) {
            ctx.setAttachment( ex );
        }

        finally {
            if ( threadState != null ) {
                threadState.clear();
                // logger.info("Security subject unbound from thread");
            }
        }
    }


    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) {
        logger.warn( "Unexpected exception from downstream.", e.getCause() );
        e.getChannel().close();
    }


    public OpReply handleMessage( ChannelHandlerContext ctx, MessageEvent e, Message message ) {
        logger.debug( "message type: {}", message.getClass().getCanonicalName() );
        if ( message instanceof OpCrud ) {
            return ( ( OpCrud ) message ).doOp( this, ctx, e );
        }

        OpReply reply = new OpReply( message );
        return reply;
    }
}
