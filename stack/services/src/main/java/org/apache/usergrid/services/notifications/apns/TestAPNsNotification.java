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
package org.apache.usergrid.services.notifications.apns;

import com.relayrides.pushy.apns.RejectedNotificationReason;
import com.relayrides.pushy.apns.util.MalformedTokenStringException;
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification;
import com.relayrides.pushy.apns.util.TokenUtil;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * notification type for testing connections
 */
public class TestAPNsNotification extends SimpleApnsPushNotification {

    private static final Logger logger = LoggerFactory.getLogger(TestAPNsNotification.class);

    boolean hasFailed = false;

    CountDownLatch latch;

    private final Timer processTimer  =
            Metrics.newTimer(TestAPNsNotification.class, "apns_test_notification", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);
    private TimerContext timer;
    private Throwable cause;

    public static TestAPNsNotification create(String tokenString, String payload) throws RuntimeException{
        try {
            final byte[] token = TokenUtil.tokenStringToByteArray(tokenString);
            return new TestAPNsNotification( token, payload);
        }catch (MalformedTokenStringException mtse) {
            throw new RuntimeException("exception foreign byte array",mtse);
        }
    }

    /**
     * setup concurrency
     * @param latch
     */
    public void setLatch(CountDownLatch latch){
        this.latch = latch;
    }

    /**
     * get concurrency
     * @return
     */
    public CountDownLatch getLatch(){
        return latch;
    }

    /**
     * decrement countdown for concurrency
     */
    public void countdown(){
        if(latch != null){
            latch.countDown();
        }
    }

    public TestAPNsNotification( byte[] token, String payload) {
        super(token, payload, Calendar.getInstance().getTime());
        this.timer = processTimer.time();
    }

    /**
     * has this failed
     * @return
     */
    public boolean hasFailed(){
        return hasFailed;
    }

    /**
     * stop timer
     */
    public void finished(){
        this.timer.stop();
    }

    /**
     * get failure reason
     * @return cause
     */
    public Throwable getCause(){return cause;}
    /**
     * mark failure state
     * @param cause
     */
    public void setReason(Throwable cause){
        hasFailed = true; //token is definitely invalid, so don't fail
        this.cause = cause;
    }
    /**
     * mark failure state
     * @param reason
     */
    public void setReason(RejectedNotificationReason reason){
        hasFailed = reason != RejectedNotificationReason.INVALID_TOKEN; //token is definitely invalid, so don't fail
    }

}
