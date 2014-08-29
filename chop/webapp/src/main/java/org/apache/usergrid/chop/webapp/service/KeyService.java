/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp.service;

import com.google.inject.Inject;
import org.apache.usergrid.chop.api.ProviderParams;
import org.apache.usergrid.chop.webapp.dao.ProviderParamsDao;
import org.apache.usergrid.chop.webapp.service.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

public class KeyService {

    private static final Logger LOG = LoggerFactory.getLogger(KeyService.class);

    private static final String CONFIG_FILE = "chop-ui.properties";
    private static final String CONFIG_KEY = "key.files.dir";

    @Inject
    private ProviderParamsDao providerParamsDao = null;

    private String keyFilesDir;

    private String getKeyFilesDir() {

        if (keyFilesDir == null) {
            keyFilesDir = FileUtil.readProperties(CONFIG_FILE).getProperty(CONFIG_KEY);
        }

        return keyFilesDir;
    }

    public File addFile(String username, String keyPairName, String fileName) throws FileNotFoundException {
        LOG.debug("Adding file: username={}, keyPairName={}, fileName={}", new String[]{username, keyPairName, fileName});

        String dir = getKeyFilesDir() + "/" + username;
        String filePath = dir + "/" + fileName;

        addKeyFile(username, keyPairName, filePath);

        new File(dir).mkdirs();
        return new File(filePath);
    }


    private void addKeyFile( String username, String keyPairName, String filePath ) {

        ProviderParams params = providerParamsDao.getByUser( username );

        LOG.info( "{}", params );
        params.getKeys().put( keyPairName, filePath );

        save( params );
    }

    private void save(ProviderParams params) {
        try {
            providerParamsDao.save(params);
        } catch (Exception e) {
            LOG.error("Error to save key file: ", e);
        }
    }

    public Map<String, String> getKeys(String username) {

        ProviderParams params = providerParamsDao.getByUser(username);

        LOG.debug("Getting keys: username={}, keys={}", username, params.getKeys());

        return params.getKeys();
    }

    public void removeKey(String username, String keyName) {
        LOG.debug("Removing key: username={}, keyName={}", username, keyName);

        ProviderParams params = providerParamsDao.getByUser(username);
        String filePath = params.getKeys().get(keyName);

        new File(filePath).delete();
        params.getKeys().remove(keyName);

        save(params);
    }

}
