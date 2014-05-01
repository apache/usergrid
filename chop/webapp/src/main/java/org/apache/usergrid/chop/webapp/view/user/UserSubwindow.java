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

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.*;
import org.apache.usergrid.chop.api.ProviderParams;
import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.dao.ProviderParamsDao;
import org.apache.usergrid.chop.webapp.dao.UserDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicProviderParams;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("unchecked")
public class UserSubwindow extends Window {

    private static final Logger LOG = LoggerFactory.getLogger(UserSubwindow.class);

    private static final String USERNAME = "Username";
    private static final String PASSWORD = "Password";
    private static final String INSTANCE_TYPE = "Instance Type";
    private static final String ACCESS_KEY = "Access Key";
    private static final String SECRET_KEY = "Secret Key";
    private static final String IMAGE_ID = "Image Id";
    private static final String KEY_PAIR_NAME = "Key Pair Name";

    private static final String[] FIELD_NAMES = new String[]{
            USERNAME, PASSWORD, INSTANCE_TYPE, ACCESS_KEY, SECRET_KEY, IMAGE_ID, KEY_PAIR_NAME
    };

    private final UserDao userDao = InjectorFactory.getInstance(UserDao.class);
    private final ProviderParamsDao providerParamsDao = InjectorFactory.getInstance(ProviderParamsDao.class);

    private final Table userList = new Table();

    private final Button addNewUserButton = new Button("New");
    private final Button removeUserButton = new Button("Remove this user");
    private final Button saveButton = new Button("Save All");
    private final Button closeButton = new Button("Close");

    private final FormLayout userEditLayout = new FormLayout();
    private final FieldGroup editorFields = new FieldGroup();

    private final IndexedContainer userContainer;

    private final KeyListLayout keyListLayout = new KeyListLayout();


    /*
     * After UI class is created, init() is executed. You should build and wire
     * up your user interface here.
     */
    public UserSubwindow() {
        super("User Manager");
        userContainer = loadUserList();

        init();
        initLayout();
        initUserList();
        initUserEditLayout();
        initButtons();
    }


    private IndexedContainer loadUserList() {
        IndexedContainer ic = new IndexedContainer();

        for (String fieldName : FIELD_NAMES) {
            ic.addContainerProperty(fieldName, String.class, "");
        }

        List<User> users = userDao.getList();

        for (User user : users) {
            ProviderParams params = providerParamsDao.getByUser(user.getUsername());

            Object id = ic.addItem();
            ic.getContainerProperty(id, USERNAME).setValue(user.getUsername());
            ic.getContainerProperty(id, PASSWORD).setValue(user.getPassword());

            if (params != null) {
                ic.getContainerProperty(id, ACCESS_KEY).setValue(params.getAccessKey());
                ic.getContainerProperty(id, IMAGE_ID).setValue(params.getImageId());
                ic.getContainerProperty(id, INSTANCE_TYPE).setValue(params.getInstanceType());
                ic.getContainerProperty(id, SECRET_KEY).setValue(params.getSecretKey());
                ic.getContainerProperty(id, KEY_PAIR_NAME).setValue(params.getKeyName());
            } else {
                ic.getContainerProperty(id, ACCESS_KEY).setValue("");
                ic.getContainerProperty(id, IMAGE_ID).setValue("");
                ic.getContainerProperty(id, INSTANCE_TYPE).setValue("");
                ic.getContainerProperty(id, KEY_PAIR_NAME).setValue("");
                ic.getContainerProperty(id, SECRET_KEY).setValue("");
            }
        }

        return ic;
    }


    private void init() {
        setHeight("100%");
        setWidth("100%");

        center();
        setClosable(false);
        setModal(true);
    }


    private void initLayout() {

        // Root of the user interface component tree is set
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        setContent(splitPanel);

        // Build the component tree
        VerticalLayout leftLayout = new VerticalLayout();
        splitPanel.addComponent(leftLayout);
        splitPanel.addComponent(userEditLayout);
        leftLayout.addComponent(userList);

        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.addComponent(addNewUserButton);
        buttonsLayout.addComponent(saveButton);
        buttonsLayout.addComponent(closeButton);
        leftLayout.addComponent(buttonsLayout);

        // Set the contents in the left of the split panel to use all the space
        leftLayout.setSizeFull();

        // On the left side, expand the size of userList so that it uses all the space left after from bottomLeftLayout
        leftLayout.setExpandRatio(userList, 1);
        userList.setSizeFull();

        // Put a little margin around the fields in the right side editor
        userEditLayout.setMargin(true);
        userEditLayout.setVisible(false);
    }


    private void initUserEditLayout() {

        // User interface can be created dynamically to reflect underlying data
        for (String fieldName : FIELD_NAMES) {
            TextField field = new TextField(fieldName);
            userEditLayout.addComponent(field);
            field.setWidth("100%");

            // We use a FieldGroup to connect multiple components to a data source at once.
            editorFields.bind(field, fieldName);
        }

        userEditLayout.addComponent(removeUserButton);
        userEditLayout.addComponent(keyListLayout);

		/*
         * Data can be buffered in the user interface. When doing so, commit()
		 * writes the changes to the data source. Here we choose to write the
		 * changes automatically without calling commit().
		 */
        editorFields.setBuffered(false);
    }


    private void initButtons() {

        addNewUserButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {

				/*
                 * Rows in the Container data model are called Item. Here we add
				 * a new row in the beginning of the list.
				 */
                userContainer.removeAllContainerFilters();
                Object contactId = userContainer.addItemAt(0);

				/*
                 * Each Item has a set of Properties that hold values. Here we
				 * set a couple of those.
				 */
                userList.getContainerProperty(contactId, USERNAME).setValue(
                        "Username");
                userList.getContainerProperty(contactId, PASSWORD).setValue(
                        "Password");

				/* Lets choose the newly created contact to edit it. */
                userList.select(contactId);
            }
        });

        removeUserButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                Object contactId = userList.getValue();
                userList.removeItem(contactId);

                String username = (String) userList.getItem(contactId).getItemProperty(USERNAME).getValue();
                userDao.delete(username);
            }
        });

        closeButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                close();
            }
        });

        saveButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                for (Object itemId : userList.getItemIds()) {
                    String username = (String) userList.getItem(itemId).getItemProperty(USERNAME).getValue();
                    String password = (String) userList.getItem(itemId).getItemProperty(PASSWORD).getValue();

                    String instanceType = (String) userList.getItem(itemId)
                            .getItemProperty(INSTANCE_TYPE)
                            .getValue();

                    String accessKey = (String) userList.getItem(itemId).getItemProperty(ACCESS_KEY).getValue();
                    String secretKey = (String) userList.getItem(itemId).getItemProperty(SECRET_KEY).getValue();
                    String imageId = (String) userList.getItem(itemId).getItemProperty(IMAGE_ID).getValue();
                    String keyPairName = (String) userList.getItem(itemId)
                            .getItemProperty(KEY_PAIR_NAME)
                            .getValue();

                    try {
                        userDao.save(new User(username, password));
                        BasicProviderParams pParams = new BasicProviderParams(username, instanceType, accessKey,
                                secretKey, imageId, keyPairName);
                        ProviderParams old = providerParamsDao.getByUser(username);
                        if (old != null) {
                            pParams.setKeys(old.getKeys());
                        }
                        providerParamsDao.save(pParams);
                    } catch (Exception e) {
                        LOG.error("Error while saving a user: ", e);
                    }
                }

                close();
            }
        });
    }


    private void initUserList() {
        userList.setContainerDataSource(userContainer);
        userList.setVisibleColumns(new String[]{USERNAME});
        userList.setSelectable(true);
        userList.setImmediate(true);

        userList.addValueChangeListener(new Property.ValueChangeListener() {
            public void valueChange(Property.ValueChangeEvent event) {
                showSelectedUser();
            }
        });
    }


    private void showSelectedUser() {

        Object itemId = userList.getValue();
        userEditLayout.setVisible(itemId != null);

        /**
         * When a contact is selected from the list, we want to show that in our editor on the right.
         * This is nicely done by the FieldGroup that binds all the fields to the corresponding Properties
         * in our contact at once.
         */
        if (itemId == null) {
            return;
        }

        Item userItem = userList.getItem(itemId);
        editorFields.setItemDataSource(userItem);

        String username = (String) userItem.getItemProperty(USERNAME).getValue();

        keyListLayout.loadKeys(username);
    }

}
