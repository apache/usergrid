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
package org.apache.usergrid.services.assets.data;


import java.util.UUID;

import org.junit.Test;
import org.apache.usergrid.persistence.entities.Asset;

import static junit.framework.Assert.assertEquals;


/** @author zznate */
public class AssetUtilsTest {

    private static UUID appId = new UUID( 0, 1 );


    @Test
    public void buildPathOk() {
        Asset asset = new Asset();
        asset.setPath( "path/to/file" );
        asset.setUuid( UUID.randomUUID() );

        String path = AssetUtils.buildAssetKey( appId, asset );

        assertEquals( 73, path.length() );
        assertEquals( appId.toString(), path.substring( 0, 36 ) );
    }


    @Test(expected = IllegalArgumentException.class)
    public void verifyErrorsOkAssetId() {
        Asset asset = new Asset();
        AssetUtils.buildAssetKey( appId, asset );
    }


    @Test(expected = IllegalArgumentException.class)
    public void verifyErrorsOkNullAppId() {
        Asset asset = new Asset();
        asset.setUuid( UUID.randomUUID() );
        AssetUtils.buildAssetKey( null, asset );
    }
}
