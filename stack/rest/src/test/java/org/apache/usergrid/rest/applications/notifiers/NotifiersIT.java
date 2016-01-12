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
package org.apache.usergrid.rest.applications.notifiers;

import org.apache.commons.io.IOUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class NotifiersIT extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger( NotifiersIT.class );

    //private static final String VALID_CERT_FILE = "apple_push_valid_nocommit.p12";
    private static final String INVALID_CERT_FILE = "pushtest_dev_recent.p12";

    private static byte[] validCertBytes;
    private static byte[] invalidCertBytes;

    @BeforeClass
    public static void setup() throws IOException {
        //InputStream validCertStream = NotifiersIT.class.getClassLoader().getResourceAsStream(VALID_CERT_FILE);
        InputStream invalidCertStream = NotifiersIT.class.getClassLoader().getResourceAsStream(INVALID_CERT_FILE);

        //validCertBytes = IOUtils.toByteArray(validCertStream);
        invalidCertBytes = IOUtils.toByteArray(invalidCertStream);

        //validCertStream.close();
        invalidCertStream.close();
    }

    @Ignore("Pending valid certificate being committed to the source code.")
    @Test
    public void createAppleNotifierValidCertificate() {


        FormDataMultiPart form = new FormDataMultiPart()
            .field("name", "validAppleNotifier")
            .field("environment", "development")
            .field("provider", "apple")
            .field( "p12Certificate", validCertBytes, MediaType.MULTIPART_FORM_DATA_TYPE );


        ApiResponse postResponse = pathResource( getOrgAppPath( "notifiers" )).post( form );

        assertNotNull("certInfo should not be null", postResponse.getEntities().get(0).get("certInfo"));

    }

    @Test
    public void createAppleNotifierInvalidCertificate() {


        FormDataMultiPart form = new FormDataMultiPart()
            .field("name", "validAppleNotifier")
            .field("environment", "development")
            .field("provider", "apple")
            .field( "p12Certificate", invalidCertBytes, MediaType.MULTIPART_FORM_DATA_TYPE );

        // this is expected to throw an exception that we can catch and verify 400 status code
        try {
            ApiResponse postResponse = pathResource( getOrgAppPath( "notifiers" )).post( form );
        } catch (BadRequestException e){
            assertEquals(400, e.getResponse().getStatus());
        }

    }



}
