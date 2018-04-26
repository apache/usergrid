/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.rest.applications.assets.rules;


import com.amazonaws.AmazonClientException;
import com.amazonaws.SDKGlobalConfiguration;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.services.exceptions.AwsPropertiesNotFoundException;
import org.junit.Assume;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Properties;


/**
 * Created in an attempt to mark no Google cred tests as ignored.  Blocked by this issue
 * https://github.com/junit-team/junit/issues/116
 *
 * Until then, simply marks as passed, which is a bit dangerous
 */
public class NoGoogleCredsRule extends AbstractRestIT implements TestRule {

    private static final Logger logger = LoggerFactory.getLogger( NoGoogleCredsRule.class );


    @Autowired
    private Properties properties;


    public Statement apply( final Statement base, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {


                try {

                    final String filename = System.getenv( "GOOGLE_APPLICATION_CREDENTIALS" );
                    logger.info("cred filename: {}", filename);
                    // if the file doesn't exist, an exception will be thrown
                    new FileInputStream(filename);

                    base.evaluate();

                }
                catch ( Throwable t ) {

                    if ( !isMissingCredsException( t ) ) {
                        throw t;
                    }

                    //do this so our test gets marked as ignored.  Not pretty, but it works
                    Assume.assumeTrue( false );


                }
            }
        };
    }


    private boolean isMissingCredsException( final Throwable t ) {

        // either no filename was provided or the filename provided doesn't actually exist on the file system
        if ( t instanceof FileNotFoundException || t instanceof NullPointerException ) {
                return true;
            }


        /**
         * Handle the multiple failure junit trace
         */
        if( t instanceof MultipleFailureException ){
            for(final Throwable failure : ((MultipleFailureException)t).getFailures()){
                final boolean isMissingCreds = isMissingCredsException( failure );

                if(isMissingCreds){
                    return true;
                }
            }
        }
        final Throwable cause = t.getCause();

        if ( cause == null ) {
            return false;
        }


        return isMissingCredsException( cause );
    }
}
