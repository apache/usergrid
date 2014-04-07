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
package org.apache.usergrid.management.cassandra;


import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import org.apache.usergrid.management.export.S3Export;


/**
 * Streams / reads the information written from the export service to a file named "test.json"
 */
public class MockS3ExportImpl implements S3Export {
    private final String filename;

    public MockS3ExportImpl (String filename) {
        this.filename = filename;
    }

    @Override
    public void copyToS3( File ephemeral, final Map<String,Object> exportInfo, String filename ) {

        File verfiedData = new File( this.filename );
        try {
            FileUtils.copyFile(ephemeral,verfiedData);
            ephemeral.delete();
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFilename () {
        return filename;
    }

}
