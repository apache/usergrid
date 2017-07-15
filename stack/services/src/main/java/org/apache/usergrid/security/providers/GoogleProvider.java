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
package org.apache.usergrid.security.providers;


import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.security.tokens.exceptions.BadTokenException;
import org.apache.usergrid.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.usergrid.persistence.Schema.PROPERTY_MODIFIED;
import static org.apache.usergrid.utils.ListUtils.anyNull;


/**
 * Provider implementation for sign-in-as with facebook
 *
 * @author zznate
 */
@SuppressWarnings("Duplicates")
public class GoogleProvider extends AbstractProvider {
    private static final String DEF_API_URL = "https://www.googleapis.com/userinfo/v2/me";

    private static final Logger logger = LoggerFactory.getLogger(GoogleProvider.class);

    private String apiUrl = DEF_API_URL;


    GoogleProvider(EntityManager entityManager, ManagementService managementService) {
        super(entityManager, managementService);
    }


    @Override
    void configure() {
        try {
            Map config = loadConfigurationFor("googleProvider");
            if (config != null) {
                String foundApiUrl = (String) config.get("google_api_url");
                if (foundApiUrl != null) {
                    apiUrl = foundApiUrl;
                }
            }
        } catch (Exception ex) {
            logger.error("Error in configure()", ex);
        }
    }


    @Override
    public Map<Object, Object> loadConfigurationFor() {
        return loadConfigurationFor("googleProvider");
    }


    /**
     * Configuration parameters we look for: <ul> <li>api_url</li> <li>pic_url</li> </ul>
     */
    @Override
    public void saveToConfiguration(Map<String, Object> config) {
        saveToConfiguration("googleProvider", config);
    }


    @SuppressWarnings("unchecked")
    @Override
    Map<String, Object> userFromResource(String externalToken) {

        return client.target(apiUrl)
                .queryParam("access_token", externalToken)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(Map.class);
    }


    @Override
    public User createOrAuthenticate(String externalToken) throws BadTokenException {

        Map<String, Object> google_user = userFromResource(externalToken);

        String id = (String) google_user.get("id");
        String user_name = (String) google_user.get("name");
        String user_email = (String) google_user.get("email");
        String user_picture = (String) google_user.get("picture");

        if (logger.isDebugEnabled()) {
            logger.debug("FacebookProvider.createOrAuthenticate: {}", JsonUtils.mapToFormattedJsonString(google_user));
        }

        User user = null;

        if ((google_user != null) && !anyNull(id, user_name)) {

            Results r = null;
            try {
                final Query query = Query.fromEquals("facebook.id", id);
                r = entityManager.searchCollection(entityManager.getApplicationRef(), "users", query);
            } catch (Exception ex) {
                throw new BadTokenException("Could not lookup user for that Facebook ID", ex);
            }
            if (r.size() > 1) {
                logger.error("Multiple users for FB ID: {}", id);
                throw new BadTokenException("multiple users with same Facebook ID");
            }

            if (r.size() < 1) {
                Map<String, Object> properties = new LinkedHashMap<String, Object>();

                properties.put("google_plus", google_user);
                properties.put("username", "fb_" + id);
                properties.put("name", user_name);
                properties.put("picture", String.format(user_picture, id));

                if (user_email != null) {
                    try {
                        user = managementService.getAppUserByIdentifier(entityManager.getApplication().getUuid(),
                                Identifier.fromEmail(user_email));
                    } catch (Exception ex) {
                        throw new BadTokenException(
                                "Could not find existing user for this applicaiton for email: " + user_email, ex);
                    }
                    // if we found the user by email, unbind the properties from above
                    // that will conflict
                    // then update the user
                    if (user != null) {
                        properties.remove("username");
                        properties.remove("name");
                        try {
                            entityManager.updateProperties(user, properties);
                        } catch (Exception ex) {
                            throw new BadTokenException("Could not update user with new credentials", ex);
                        }
                        user.setProperty(PROPERTY_MODIFIED, properties.get(PROPERTY_MODIFIED));
                    } else {
                        properties.put("email", user_email);
                    }
                }
                if (user == null) {
                    properties.put("activated", true);
                    try {
                        user = entityManager.create("user", User.class, properties);
                    } catch (Exception ex) {
                        throw new BadTokenException("Could not create user for that token", ex);
                    }
                }
            } else {
                user = (User) r.getEntity().toTypedEntity();
                Map<String, Object> properties = new LinkedHashMap<String, Object>();

                properties.put("facebook", google_user);
                properties.put("picture", String.format(user_picture, id));
                try {
                    entityManager.updateProperties(user, properties);
                    user.setProperty(PROPERTY_MODIFIED, properties.get(PROPERTY_MODIFIED));
                    user.setProperty("facebook", google_user);
                    user.setProperty("picture", String.format(user_picture, id));
                } catch (Exception ex) {
                    throw new BadTokenException("Could not update user properties", ex);
                }
            }
        } else {
            throw new BadTokenException("Unable to confirm Facebook access token");
        }

        return user;
    }
}
