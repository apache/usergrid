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

package org.apache.usergrid.services.notifications;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.notifications.apns.APNsAdapter;
import org.apache.usergrid.services.notifications.gcm.GCMAdapter;
import org.apache.usergrid.services.notifications.wns.WNSAdapter;


/**
 * Get valid provideradapters
 */
public class ProviderAdapterFactory {
    private static final String[] providers =  new String[]{"apple", "google", "noop"};
   public static ProviderAdapter getProviderAdapter(Notifier notifier, EntityManager entityManager){
       ProviderAdapter adapter = null;
       switch(notifier.getProvider().toLowerCase()){
           case "apple" : adapter = new APNsAdapter(entityManager,notifier); break;
           case "google" : adapter = new GCMAdapter(entityManager ,notifier); break;
           case "windows" : adapter = new WNSAdapter(entityManager ,notifier); break;
           case "noop" : adapter = new TestAdapter(notifier); break;
           default: throw new IllegalArgumentException(notifier.getProvider()
               + " did not match any known adapter, valid arguments are apple,google,windows" //ignore noop its internal
           );
       }
       return adapter;

   }

    public static String[] getValidProviders() {
        return providers;
    }
}
