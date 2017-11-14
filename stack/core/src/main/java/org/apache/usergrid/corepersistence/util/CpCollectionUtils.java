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

import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.queue.settings.IndexConsistency;
import org.apache.usergrid.persistence.queue.settings.QueueIndexingStrategy;


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
    public static final String SETTING_INDEX_CONSISTENCY = "indexConsistency";

    private static Set<String> VALID_SETTING_NAMES = new HashSet<>();

    static {
        VALID_SETTING_NAMES.add(SETTING_FIELDS);
        VALID_SETTING_NAMES.add(SETTING_QUEUE_INDEX);
        VALID_SETTING_NAMES.add(SETTING_INDEX_CONSISTENCY);
    }

    public static Set<String> getValidSettings() {
        return VALID_SETTING_NAMES;
    }

    // When running in debug mode we allow some normally invalid index settings
    // like update C* but not ES.
    private static boolean debugMode = false;
    public static boolean getDebugMode() {
        return debugMode;
    }

    public static void setDebugMode(boolean set) {
        debugMode = set;
    }

    public static Object validateValue(String name, Object value) {
        if (SETTING_QUEUE_INDEX.equals(name)) {
            return QueueIndexingStrategy.get(value.toString()).getName();
        }
        if (SETTING_INDEX_CONSISTENCY.equals(name)) {
            return IndexConsistency.get(value.toString()).getName();
        }
        return value;
    }

    public static QueueIndexingStrategy getIndexingStrategyForType(CollectionSettingsFactory collectionSettingsFactory, UUID applicationId, String type ) {

        QueueIndexingStrategy queueIndexingStrategy = QueueIndexingStrategy.CONFIG;
        String indexing = getFieldForType(applicationId, collectionSettingsFactory, type, SETTING_QUEUE_INDEX);
        if (indexing != null) {
            queueIndexingStrategy = QueueIndexingStrategy.get(indexing);
        }
        return queueIndexingStrategy;
    }


    public static IndexConsistency getIndexConsistencyForType(CollectionSettingsFactory collectionSettingsFactory, UUID applicationId, String type ) {

        IndexConsistency indexConsistency = IndexConsistency.STRICT;
        String indexConsistencyString = getFieldForType(applicationId, collectionSettingsFactory, type, SETTING_INDEX_CONSISTENCY);
        if ( indexConsistencyString != null) {
            indexConsistency = IndexConsistency.get(indexConsistencyString);
        }
        return indexConsistency;
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
