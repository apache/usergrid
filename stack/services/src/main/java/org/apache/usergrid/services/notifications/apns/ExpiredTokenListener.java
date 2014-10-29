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

import com.relayrides.pushy.apns.ExpiredToken;
import com.relayrides.pushy.apns.PushManager;
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification;
import org.apache.usergrid.services.notifications.InactiveDeviceManager;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Listen for token expirations and remove
 */
public class ExpiredTokenListener implements com.relayrides.pushy.apns.ExpiredTokenListener<SimpleApnsPushNotification> {


    @Override
    public void handleExpiredTokens(PushManager<? extends SimpleApnsPushNotification> pushManager, Collection<ExpiredToken> expiredTokens) {
        Map<String,Date> inactiveDeviceMap = new HashMap<>();
        for(ExpiredToken token : expiredTokens){
            String expiredToken = new String(token.getToken());
            inactiveDeviceMap.put(expiredToken, token.getExpiration());
        }
        if(pushManager instanceof EntityPushManager){
            EntityPushManager entityPushManager = (EntityPushManager) pushManager;
            InactiveDeviceManager inactiveDeviceManager = new InactiveDeviceManager(entityPushManager.getNotifier(),entityPushManager.getEntityManager());
            inactiveDeviceManager.removeInactiveDevices(inactiveDeviceMap);
        }
    }
}
