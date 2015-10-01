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
package org.apache.usergrid.chop.webapp.view.user;

import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.service.KeyService;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class KeyListLayout extends AbsoluteLayout implements Upload.Receiver, Upload.SucceededListener {

    private static final Logger LOG = LoggerFactory.getLogger(KeyListLayout.class);

    private final KeyService keyService = InjectorFactory.getInstance(KeyService.class);

    private final TextField keyNameField = new TextField("Key Pair Name:");

    private final int START_TOP = 160;

    private final ArrayList<String> keyNames = new ArrayList<String>();
    private final ArrayList<Label> keyLabels = new ArrayList<Label>();
    private final ArrayList<Button> keyRemoveButtons = new ArrayList<Button>();

    private String username;

    KeyListLayout() {
        init();
        addTitleLabel();
        addUploadControls();
    }

    private void init() {
        setHeight("400px");
        setWidth("500px");
    }

    void addTitleLabel() {
        UIUtil.addLabel(this, "<b>Manage Keys</b>", "left: 0px; top: 10px;", "120px");
    }

    void addUploadControls() {

        keyNameField.setWidth("290px");
        keyNameField.setValue("key-pair-name");
        addComponent(keyNameField, "left: 0px; top: 50px;");

        Upload upload = new Upload("", this);
        upload.setButtonCaption("Add");
        upload.addSucceededListener(this);

        addComponent(upload, "left: 0px; top: 80px;");
    }

    @Override
    public OutputStream receiveUpload(String fileName, String mimeType) {

        FileOutputStream outStream = null;

        try {
            File file = keyService.addFile(username, keyNameField.getValue(), fileName);
            outStream = new FileOutputStream(file);
            /** SSH requires that key files only have read-write permissions for OWNER */
            Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>();
            permissions.add( PosixFilePermission.OWNER_READ );
            permissions.add( PosixFilePermission.OWNER_WRITE );
            Files.setPosixFilePermissions( file.toPath(), permissions );
        }
        catch ( IOException e ) {
            LOG.error( "Error while setting key file permissions.", e );
        }

        return outStream;
    }

    @Override
    public void uploadSucceeded(Upload.SucceededEvent succeededEvent) {
        loadKeys();
        Notification.show("Upload", "File uploaded!", Notification.Type.TRAY_NOTIFICATION);
    }

    public void loadKeys(String username) {
        this.username = username;
        loadKeys();
    }

    private void clearKeys() {
        for (Label label : keyLabels) {
            removeComponent(label);
        }

        for (Button button : keyRemoveButtons) {
            removeComponent(button);
        }

        keyNames.clear();
        keyLabels.clear();
        keyRemoveButtons.clear();
    }

    private void loadKeys() {

        clearKeys();

        Map<String, String> keys = keyService.getKeys(username);
        int top = START_TOP;

        for (Map.Entry<String, String> e : keys.entrySet()) {
            keyNames.add(e.getKey());
            addKeyLabel(e.getKey(), e.getValue(), top);
            addKeyRemoveButton(e.getKey(), top);
            top += 20;
        }
    }

    private void addKeyLabel(String keyPairName, String filePath, int top) {

        String text = String.format("%s: %s", keyPairName, filePath);
        String position = String.format("left: 30px; top: %spx;", top);

        Label label = UIUtil.addLabel(this, text, position, "500px");
        keyLabels.add(label);
    }

    private void addKeyRemoveButton(final String keyName, int top) {

        String position = String.format("left: 0px; top: %spx;", top);

        Button button = UIUtil.addButton(this, "[X]", position, "25px");
        button.setStyleName(Reindeer.BUTTON_LINK);

        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                removeKey(keyName);
            }
        });

        keyRemoveButtons.add(button);
    }

    private void removeKey(String keyName) {
        keyService.removeKey(username, keyName);
        loadKeys();
    }

    public void disableKeyLabels(){
        for ( Label l : keyLabels ){
            l.setVisible( false );
        }
    }
}

