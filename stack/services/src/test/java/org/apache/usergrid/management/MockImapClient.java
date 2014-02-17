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
package org.apache.usergrid.management;


import java.io.IOException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MockImapClient {

    private static final Logger logger = LoggerFactory.getLogger( MockImapClient.class );

    String host;
    String user;
    String password;


    public MockImapClient( String host, String user, String password ) {
        this.host = host;
        this.user = user;
        this.password = password;
    }


    public void processMail() {
        try {
            Session session = getMailSession();
            Store store = connect( session );
            Folder folder = openMailFolder( store );
            findContent( folder );
        }
        catch ( MessagingException e ) {
            throw new RuntimeException( e );
        }
        catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }


    public Session getMailSession() {
        Properties props = System.getProperties();
        props.setProperty( "mail.transport.protocol", "smtp" );
        props.setProperty( "mail.store.protocol", "imap" );
        props.setProperty( "mail.imap.partialfetch", "0" );

        logger.info( "Getting session" );
        return Session.getDefaultInstance( props, null );
    }


    public Store connect( Session session ) throws MessagingException {
        logger.info( "getting the session for accessing email." );
        Store store = session.getStore( "imap" );

        store.connect( host, user, password );
        logger.info( "Connection established with IMAP server." );
        return store;
    }


    public Folder openMailFolder( Store store ) throws MessagingException {
        Folder folder = store.getDefaultFolder();
        folder = folder.getFolder( "inbox" );
        folder.open( Folder.READ_ONLY );
        return folder;
    }


    public void findContent( Folder folder ) throws MessagingException, IOException {
        for ( Message m : folder.getMessages() ) {
            logger.info( "Subject: " + m.getSubject() );
        }
    }
}
