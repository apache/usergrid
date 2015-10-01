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
package org.apache.usergrid.chop.runner;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Servlet configuration information.
 */
@FigSingleton
public interface ServletFig extends GuicyFig {
    String CONTEXT_PATH = "context.path";

    @Key( ServletFig.CONTEXT_PATH )
    String getContextPath();


    String SERVER_INFO_KEY = "server.info";

    @Key( ServletFig.SERVER_INFO_KEY )
    String getServerInfo();



    String CONTEXT_TEMPDIR_KEY = "javax.servlet.context.tempdir";

    @Key( ServletFig.CONTEXT_TEMPDIR_KEY )
    String getContextTempDir();


    /** prop key for number of times to retry recovery operations */
    String RECOVERY_RETRY_COUNT_KEY = "recovery.retry.count";
    /** default for number of times to retry recovery operations */
    String DEFAULT_RECOVERY_RETRY_COUNT = "3";

    /**
     * Gets the number of times to retry recovery operations. Uses {@link
     * ServletFig#RECOVERY_RETRY_COUNT_KEY} to access the retry count.
     *
     * @return the number of retries for recovery
     */
    @Default( ServletFig.DEFAULT_RECOVERY_RETRY_COUNT )
    @Key( ServletFig.RECOVERY_RETRY_COUNT_KEY )
    int getRecoveryRetryCount();


    /** prop key for the time to wait between retry recovery operations */
    String DELAY_RETRY_KEY = "recovery.retry.delay";
    /** default for the time to wait in milliseconds between retry recovery operations */
    String DEFAULT_DELAY_RETRY = "10000";

    /**
     * Gets the amount of time to wait between retry operations. Uses {@link
     * ServletFig#DELAY_RETRY_KEY} to access the recovery delay.
     *
     * @return the time in milliseconds to delay between retry operations
     */
    @Default( ServletFig.DEFAULT_DELAY_RETRY )
    @Key( ServletFig.DELAY_RETRY_KEY )
    long getRetryDelay();
}
