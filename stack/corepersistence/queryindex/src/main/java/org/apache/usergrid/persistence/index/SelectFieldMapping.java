/*
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.apache.usergrid.persistence.index;


import java.io.Serializable;


/**
 * A simple object that represents a field mapping.
 *
 * Examples:
 *  "select id", which will return id
 *
 *  "select id:appfield" which will return id, but rename it appfield
 */
public class SelectFieldMapping implements Serializable {

    private final String sourceFieldName;
    private final String targetFieldName;


    public SelectFieldMapping( final String sourceFieldName, final String targetFieldName ) {
        this.sourceFieldName = sourceFieldName;
        this.targetFieldName = targetFieldName;
    }


    public String getSourceFieldName() {
        return sourceFieldName;
    }


    public String getTargetFieldName() {
        return targetFieldName;
    }
}
