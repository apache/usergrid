/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.services.notifications.apns;

import com.relayrides.pushy.apns.ApnsEnvironment;
import com.relayrides.pushy.apns.PushManager;
import com.relayrides.pushy.apns.PushManagerConfiguration;
import com.relayrides.pushy.apns.util.SSLContextUtil;
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Notifier;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Store notifier within PushManager so it can be retrieved later.  Need this for the async token listener
 */
public class EntityPushManager extends PushManager<SimpleApnsPushNotification> {
    private final Notifier notifier;
    private final EntityManager entityManager;

    public EntityPushManager( Notifier notifier, EntityManager entityManager, BlockingQueue<SimpleApnsPushNotification> queue, PushManagerConfiguration configuration) {
        super(getApnsEnvironment(notifier), getSSLContext(notifier), null, null, queue, configuration, notifier.getName());
        this.notifier = notifier;
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public Notifier getNotifier() {
        return notifier;
    }
    private static ApnsEnvironment getApnsEnvironment(Notifier notifier){
        return  notifier.isProduction()
                ? ApnsEnvironment.getProductionEnvironment()
                : ApnsEnvironment.getSandboxEnvironment();
    }
    private static SSLContext getSSLContext(Notifier notifier) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String password = notifier.getCertificatePassword();
            char[] passChars =(password != null ? password : "").toCharArray();
            InputStream stream = notifier.getP12CertificateStream();
            keyStore.load(stream,passChars);
            SSLContext context =  SSLContextUtil.createDefaultSSLContext(keyStore, passChars);
            return context;
        }catch (Exception e){
            throw new RuntimeException("Error getting certificate",e);
        }
    }
}
