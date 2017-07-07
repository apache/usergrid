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
package org.apache.usergrid.utils;


import org.apache.usergrid.corepersistence.index.CollectionVersionUtil;
import org.apache.usergrid.corepersistence.index.VersionedCollectionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InflectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(InflectionUtils.class );

    private static VersionedCollectionName parseName(Object word) {
        String name = word.toString().trim();
        try {
            return CollectionVersionUtil.parseVersionedName(name);
        }
        catch (Exception e) {
            logger.error("parseName(): failed to parse the versioned name: {}", name);
            return CollectionVersionUtil.createVersionedName(name, "");
        }
    }

    private static String getVersionedName(String name, String version) {
        try {
            return CollectionVersionUtil.buildVersionedNameString(name, version, true);
        }
        catch (Exception e) {
            // if versioned invalid, return name
            return name;
        }
    }

    public static String pluralize( Object word ) {
        VersionedCollectionName name = parseName(word);
        String pluralizedName = Inflector.INSTANCE.pluralize(name.getCollectionName());
        return getVersionedName(pluralizedName, name.getCollectionVersion());
    }


    public static String singularize( Object word ) {
        VersionedCollectionName name = parseName(word);
        String singuralizedName = Inflector.INSTANCE.singularize(name.getCollectionName());
        return getVersionedName(singuralizedName, name.getCollectionVersion());
    }


    public static boolean isPlural( Object word ) {
        VersionedCollectionName name = parseName(word);
        return Inflector.INSTANCE.isPlural( name.getCollectionName() );
    }


    public static boolean isSingular( Object word ) {
        VersionedCollectionName name = parseName(word);
        return Inflector.INSTANCE.isSingular( name.getCollectionName() );
    }


    public static String underscore( String s ) {
        return Inflector.INSTANCE.underscore( s );
    }


    public static String camelCase( String lowerCaseAndUnderscoredWord, boolean uppercaseFirstLetter,
                                    char... delimiterChars ) {
        return Inflector.INSTANCE.camelCase( lowerCaseAndUnderscoredWord, uppercaseFirstLetter, delimiterChars );
    }
}
