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
package org.apache.usergrid.utils;


import java.util.List;
import java.util.Properties;

import javax.mail.Message;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class MailUtilsTest {

    private Properties props;
    private MailUtils mailUtils;


    @Before
    public void setupLocal() {
        props = new Properties();
        props.put( "mail.transport.protocol", "smtp" );
        props.put( "mail.smtp.host", "usergrid.com" );
        props.put( "mail.smtp.username", "testuser" );
        props.put( "mail.smtp.password", "testpassword" );

        mailUtils = new MailUtils();
    }


    @Test
    public void verifyConstructionOk() throws Exception {
        String email = consistentEmail();

        sendTestEmail( email );

        List<Message> userInbox = org.jvnet.mock_javamail.Mailbox.get( email );

        assertEquals( 1, userInbox.size() );
    }


    @Test
    public void failedConstruction() throws Exception {

        sendTestEmail( "foo@bar." );
    }


    @Test
    public void propertiesExtraction() {
        props.put( "some.other.prop", "foo" );
        assertEquals( 5, props.size() );
        assertEquals( 4, MailUtils.getMailProperties( props ).size() );
    }


    @Test(expected = IllegalArgumentException.class)
    public void verifyMissingTemplateFail() {
        mailUtils.sendHtmlMail( props, "foo@bar", "user@usergrid.com", "", "" );
    }


    private void sendTestEmail( String email ) {
        mailUtils.sendHtmlMail( props, email, "user@usergrid.com", "test subject", "Email body" );
    }


    private String consistentEmail() {
        return String.format( "user-%s@mockserver.com", UUIDUtils.newTimeUUID().toString() );
    }
}
