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

package org.apache.usergrid.management.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MockS3ImportImpl implements S3Import{
    private static final Logger logger = LoggerFactory.getLogger(MockS3ImportImpl.class);

    @Override
    public List<String> getBucketFileNames(String bucketName, String endsWith, String accessId, String secretKey) {
        return new ArrayList<>();
    }

    @Override
    public File copyFileFromBucket(String blobFileName, String bucketName, String accessId, String secretKey) throws IOException {
        return File.createTempFile("test","tmp");
    }

}
