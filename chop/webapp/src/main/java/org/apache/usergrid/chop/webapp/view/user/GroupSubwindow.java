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

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents;
import com.vaadin.ui.*;

@SuppressWarnings("unchecked")
class GroupSubwindow extends Window {

    /* User interface components are stored in session. */
    private Table groupList = new Table();
    private TextField searchField = new TextField();
    private Button addNewGroupButton = new Button("New");
    private Button removeGroupButton = new Button("Remove this group");
    private Button saveButton = new Button("Save");
    private Button cancelButton = new Button("Cancel");
    private FormLayout editorLayout = new FormLayout();
    private FieldGroup editorFields = new FieldGroup();

    private static final String GROUP = "Group";
    private static final String[] fieldNames = new String[]{GROUP};

    IndexedContainer groupContainer;
    String username;

    public GroupSubwindow(String username) {
        super(String.format("Edit %s's groups", username)); // Set window caption
        center();
        setClosable(false);
        setModal(true);

        // Set window size.
        setHeight("100%");
        setWidth("100%");

        this.username = username;
        groupContainer = createGroupDatasource();

        initLayout();
        initContactList();
        initEditor();
        initSearch();
        initButtons();
    }

    private void initLayout() {

		/* Root of the user interface component tree is set */
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        setContent(splitPanel);

		/* Build the component tree */
        VerticalLayout leftLayout = new VerticalLayout();
        splitPanel.addComponent(leftLayout);
        splitPanel.addComponent(editorLayout);
        leftLayout.addComponent(groupList);
        HorizontalLayout bottomLeftLayout = new HorizontalLayout();
        leftLayout.addComponent(bottomLeftLayout);
        bottomLeftLayout.addComponent(searchField);
        bottomLeftLayout.addComponent(addNewGroupButton);
        bottomLeftLayout.addComponent(saveButton);
        bottomLeftLayout.addComponent(cancelButton);

		/* Set the contents in the left of the split panel to use all the space */
        leftLayout.setSizeFull();

		/*
         * On the left side, expand the size of the userList so that it uses
		 * all the space left after from bottomLeftLayout
		 */
        leftLayout.setExpandRatio(groupList, 1);
        groupList.setSizeFull();

		/*
         * In the bottomLeftLayout, searchField takes all the width there is
		 * after adding addNewUserButton. The height of the layout is defined
		 * by the tallest component.
		 */
        bottomLeftLayout.setWidth("100%");
        searchField.setWidth("100%");
        bottomLeftLayout.setExpandRatio(searchField, 1);

		/* Put a little margin around the fields in the right side editor */
        editorLayout.setMargin(true);
        editorLayout.setVisible(false);
    }

    private void initEditor() {

        editorLayout.addComponent(removeGroupButton);

		/* User interface can be created dynamically to reflect underlying data. */
        for (String fieldName : fieldNames) {
            TextField field = new TextField(fieldName);
            editorLayout.addComponent(field);
            field.setWidth("100%");

			/*
             * We use a FieldGroup to connect multiple components to a data
			 * source at once.
			 */
            editorFields.bind(field, fieldName);
        }

		/*
         * Data can be buffered in the user interface. When doing so, commit()
		 * writes the changes to the data source. Here we choose to write the
		 * changes automatically without calling commit().
		 */
        editorFields.setBuffered(false);
    }

    private void initSearch() {

		/*
         * We want to show a subtle prompt in the search field. We could also
		 * set a caption that would be shown above the field or description to
		 * be shown in a tooltip.
		 */
        searchField.setInputPrompt("Search group");

		/*
         * Granularity for sending events over the wire can be controlled. By
		 * default simple changes like writing a text in TextField are sent to
		 * server with the next Ajax call. You can set your component to be
		 * immediate to send the changes to server immediately after focus
		 * leaves the field. Here we choose to send the text over the wire as
		 * soon as user stops writing for a moment.
		 */
        searchField.setTextChangeEventMode(AbstractTextField.TextChangeEventMode.LAZY);

		/*
         * When the event happens, we handle it in the anonymous inner class.
		 * You may choose to use separate controllers (in MVC) or presenters (in
		 * MVP) instead. In the end, the preferred application architecture is
		 * up to you.
		 */
        searchField.addTextChangeListener(new FieldEvents.TextChangeListener() {
            public void textChange(final FieldEvents.TextChangeEvent event) {

				/* Reset the filter for the userContainer. */
                groupContainer.removeAllContainerFilters();
                groupContainer.addContainerFilter(new ContactFilter(event
                        .getText()));
            }
        });
    }

    /*
     * A custom filter for searching names and companies in the
     * userContainer.
     */
    private class ContactFilter implements Container.Filter {
        private String needle;

        public ContactFilter(String needle) {
            this.needle = needle.toLowerCase();
        }

        public boolean passesFilter(Object itemId, Item item) {
            String haystack = ("" + item.getItemProperty(GROUP).getValue()).toLowerCase();
            return haystack.contains(needle);
        }

        public boolean appliesToProperty(Object id) {
            return true;
        }
    }

    private void initButtons() {
        addNewGroupButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {

				/*
                 * Rows in the Container data model are called Item. Here we add
				 * a new row in the beginning of the list.
				 */
                groupContainer.removeAllContainerFilters();
                Object contactId = groupContainer.addItemAt(0);

				/*
                 * Each Item has a set of Properties that hold values. Here we
				 * set a couple of those.
				 */
                groupContainer.getContainerProperty(contactId, GROUP).setValue(
                        "New Group");

				/* Lets choose the newly created contact to edit it. */
                groupList.select(contactId);
            }
        });

        removeGroupButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                Object contactId = groupList.getValue();
                groupList.removeItem(contactId);
            }
        });

        cancelButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                close(); // Close the sub-window
            }
        });

        saveButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {

                /*Set<String> groups = MyShiroRealm.getUserRoles(username);
                groups.clear();

                for(Object itemId : groupList.getItemIds()){
                    String gname = (String) groupList.getItem(itemId).getItemProperty(GROUP).getValue();
                    groups.add(gname);
                }

                close(); // Close the sub-window
                MyShiroRealm.saveRealm();*/
            }
        });
    }

    private void initContactList() {
        groupList.setContainerDataSource(groupContainer);
        groupList.setVisibleColumns(new String[]{GROUP});
        groupList.setSelectable(true);
        groupList.setImmediate(true);

        groupList.addValueChangeListener(new Property.ValueChangeListener() {
            public void valueChange(Property.ValueChangeEvent event) {
                Object contactId = groupList.getValue();

				/*
                 * When a contact is selected from the list, we want to show
				 * that in our editor on the right. This is nicely done by the
				 * FieldGroup that binds all the fields to the corresponding
				 * Properties in our contact at once.
				 */
                if (contactId != null) {
                    editorFields.setItemDataSource(groupList
                            .getItem(contactId));
                }

                editorLayout.setVisible(contactId != null);
            }
        });
    }

    /*
     * Generate some in-memory example data to play with. In a real application
     * we could be using SQLContainer, JPAContainer or some other to persist the
     * data.
     */
    private IndexedContainer createGroupDatasource() {
        IndexedContainer ic = new IndexedContainer();

        for (String p : fieldNames) {
            ic.addContainerProperty(p, String.class, "");
        }

        /*Set<String> groups = MyShiroRealm.getUserRoles(this.username);

        if (groups != null && !groups.isEmpty()) {
            for (String gname : groups) {
                if(!Strings.isNullOrEmpty(gname.trim())){
                    Object id = ic.addItem();
                    ic.getContainerProperty(id, GROUP).setValue(
                            gname.trim());
                }
            }
        }*/

        return ic;
    }
}
