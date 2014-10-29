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
package org.apache.usergrid.services.notifiers;

import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.notifications.ProviderAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.services.*;
import org.apache.usergrid.services.notifications.NotificationsService;
import org.apache.usergrid.services.notifications.ProviderAdapter;
import java.util.Arrays;

public class NotifiersService extends AbstractCollectionService {

    private static final Logger logger = LoggerFactory
            .getLogger(NotifiersService.class);

    public NotifiersService() {
        super();
        logger.info("/notifiers");
    }

    @Override
    public ServiceResults postCollection(ServiceContext context)
            throws Exception {
        ServicePayload payload = context.getPayload();
        ServiceResults results = super.postCollection(context);
        Notifier notifier = (Notifier) results.getEntity();
        if (notifier != null) {
            try {
                ProviderAdapter providerAdapter = ProviderAdapterFactory.getProviderAdapter(notifier, em);

                if (providerAdapter==null) {
                    throw new IllegalArgumentException("provider must be one of: "
                            + Arrays.toString(ProviderAdapterFactory.getValidProviders()));
                }
                providerAdapter.validateCreateNotifier(payload);
                NotificationsService ns = (NotificationsService) sm.getService("notifications");
                ns.testConnection(notifier);
            } catch (Exception e) {
                logger.info("notifier testConnection() failed", e);
                em.delete(notifier);
                throw e;
            }
        }

        return results;
    }
}
