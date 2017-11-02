/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.util;

import com.google.common.base.Optional;
import org.apache.usergrid.corepersistence.index.CollectionSettings;
import org.apache.usergrid.corepersistence.index.CollectionSettingsFactory;
import org.apache.usergrid.corepersistence.index.CollectionSettingsScopeImpl;

import org.apache.usergrid.corepersistence.index.IndexingStrategy;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;


import java.util.*;

import static org.apache.usergrid.persistence.Schema.*;


/**
 *
 * Helper methods to manage the Collection setting properties
 *
 */
public class CpCollectionUtils {


    public static final String SETTING_FIELDS = "fields";
    public static final String SETTING_QUEUE_INDEX = "queueIndex";

    private static Set<String> VALID_SETTING_NAMES = new HashSet<>();

    static {
        VALID_SETTING_NAMES.add(SETTING_FIELDS);
        VALID_SETTING_NAMES.add(SETTING_QUEUE_INDEX);
    }

    public static Set<String> getValidSettings() {
        return VALID_SETTING_NAMES;
    }

    public static IndexingStrategy getIndexingStrategyForType(CollectionSettingsFactory collectionSettingsFactory, UUID applicationId, String type ) {

        IndexingStrategy indexingStrategy = IndexingStrategy.DEFAULT;
        String indexing = getFieldForType(applicationId, collectionSettingsFactory, type, SETTING_QUEUE_INDEX);
        if (indexing != null) {
            indexingStrategy = IndexingStrategy.get(indexing);
        }
        return indexingStrategy;
    }

    public static Boolean asyncIndexingForType(CollectionSettingsFactory collectionSettingsFactory, UUID applicationId, String type ) {

        String indexing = getFieldForType(applicationId, collectionSettingsFactory, type, SETTING_QUEUE_INDEX);
        if ("async".equals(indexing)) {
            return Boolean.TRUE;
        }
        if ("sync".equals(indexing)) {
            return Boolean.FALSE;
        }
        return null;
    }

    public static boolean skipIndexingForType(CollectionSettingsFactory collectionSettingsFactory, UUID applicationId, String type ) {

        String fields = getFieldForType(applicationId, collectionSettingsFactory, type, SETTING_FIELDS);
        boolean skipIndexing = false;
        if ( fields != null && fields instanceof String && "none".equalsIgnoreCase( fields.toString())) {
            skipIndexing = true;
        }

        return skipIndexing;
    }

    // these same methods are in CpEntityManager must refactor
    private static String getFieldForType(UUID applicationId, CollectionSettingsFactory collectionSettingsFactory,
                                          String type, String keyName ) {

        String collectionName = Schema.defaultCollectionName( type );

        CollectionSettings collectionSettings = collectionSettingsFactory
            .getInstance( new CollectionSettingsScopeImpl(getAppIdObject(applicationId), collectionName) );
        Optional<Map<String, Object>> existingSettings =
            collectionSettings.getCollectionSettings( collectionName );

        if ( existingSettings.isPresent()) {
            Map jsonMapData = existingSettings.get();
            Object value = jsonMapData.get(keyName);
            if ( value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private static Id getAppIdObject(UUID applicationId){
        return new SimpleId( applicationId, TYPE_APPLICATION );
    }


}
