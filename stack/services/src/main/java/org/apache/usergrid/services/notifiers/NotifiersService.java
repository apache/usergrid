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

import org.apache.usergrid.persistence.DynamicEntity;
import org.apache.usergrid.persistence.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.services.*;
import org.apache.usergrid.services.notifications.NotificationsService;
import org.apache.usergrid.services.notifications.ProviderAdapter;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

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

        NotificationsService ns = (NotificationsService) sm
                .getService("notifications");
        Set<String> providers = ns.getProviders();

        String provider = payload.getStringProperty("provider");
        if (!providers.contains(provider)) {
            throw new IllegalArgumentException("provider must be one of: "
                    + Arrays.toString(providers.toArray()));
        }

        ProviderAdapter providerAdapter = ns.providerAdapters.get(provider);
        providerAdapter.validateCreateNotifier(payload);

        ServiceResults results = super.postCollection(context);

        DynamicEntity entity = (DynamicEntity) results.getEntity();
        Notifier notifier1 = new Notifier();
        notifier1.setProperties(entity.getProperties());
        if (entity != null) {
            try {
                ns.testConnection(notifier1);
            } catch (Exception e) {
                logger.info("notifier testConnection() failed", e);
                em.delete(entity);
                throw e;
            }
        }

        return results;
    }
}
