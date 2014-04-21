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
package org.apache.usergrid.persistence.schema;


import org.apache.usergrid.persistence.annotations.EntityDictionary;


public class DictionaryInfo {
    private String name;
    private Class<?> keyType;

    private Class<?> valueType; // = Long.class.getName();
    private boolean keysIndexedInConnections;
    private boolean publicVisible = true;
    private boolean includedInExport = true;


    public DictionaryInfo() {
    }


    public DictionaryInfo( EntityDictionary setAnnotation ) {
        setKeyType( setAnnotation.keyType() );
        setValueType( setAnnotation.valueType() );
        setKeysIndexedInConnections( setAnnotation.keysIndexedInConnections() );
        setPublic( setAnnotation.publicVisible() );
        setIncludedInExport( setAnnotation.includedInExport() );
    }


    public String getName() {
        return name;
    }


    public void setName( String name ) {
        this.name = name;
    }


    public Class<?> getKeyType() {
        return keyType;
    }


    public void setKeyType( Class<?> type ) {
        if ( type == Object.class ) {
            type = null;
        }
        keyType = type;
    }


    public Class<?> getValueType() {
        return valueType;
    }


    public void setValueType( Class<?> valueType ) {
        if ( valueType == Object.class ) {
            valueType = null;
        }
        this.valueType = valueType;
    }


    public boolean isKeysIndexedInConnections() {
        return keysIndexedInConnections;
    }


    public void setKeysIndexedInConnections( boolean keysIndexedInConnections ) {
        this.keysIndexedInConnections = keysIndexedInConnections;
    }


    public boolean isPublic() {
        return publicVisible;
    }


    public void setPublic( boolean publicVisible ) {
        this.publicVisible = publicVisible;
    }


    public boolean isIncludedInExport() {
        return includedInExport;
    }


    public void setIncludedInExport( boolean includedInExport ) {
        this.includedInExport = includedInExport;
    }


    @Override
    public String toString() {
        return "Set [name=" + name + ", keyType=" + keyType + ", valueType=" + valueType + ", keysIndexedInConnections="
                + keysIndexedInConnections + ", publicVisible=" + publicVisible + "]";
    }
}
