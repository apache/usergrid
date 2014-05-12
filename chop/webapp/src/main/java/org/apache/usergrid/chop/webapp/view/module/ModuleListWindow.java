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
package org.apache.usergrid.chop.webapp.view.module;


import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.view.util.PopupWindow;

import com.vaadin.event.ItemClickEvent;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.TreeTable;


public class ModuleListWindow extends PopupWindow {

    private static final String PARENT_PREFIX = "parent:";

    private final ModuleSelectListener listener;

    public ModuleListWindow(ModuleSelectListener listener) {
        super( "Modules" );
        this.listener = listener;
    }


    @Override
    protected void addItems(AbsoluteLayout mainLayout) {

        TreeTable treeTable = getTree();
        treeTable.setWidth( "100%" );
        treeTable.setHeight( "420px" );

        mainLayout.addComponent( treeTable, "left: 0px; top: 0px;" );
    }


    public TreeTable getTree() {

        TreeTable treeTable = new TreeTable();
        treeTable.setSelectable( true );
        treeTable.addContainerProperty( "Group", String.class, "" );
        treeTable.addContainerProperty( "Artifact", String.class, "" );
        treeTable.addItemClickListener( getItemClickListener() );

        addTreeItems( treeTable );

        return treeTable;
    }


    private ItemClickEvent.ItemClickListener getItemClickListener() {
        return new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                onItemClick(event);
            }
        };
    }


    private void onItemClick(ItemClickEvent event) {

        String id = (String) event.getItemId();
        boolean isModuleVersion = !StringUtils.startsWith( id, PARENT_PREFIX );

        if (isModuleVersion) {
            close();
            listener.onModuleSelect(id);
        }
    }


    private static void addTreeItems( TreeTable treeTable ) {

        ModuleDao moduleDao = InjectorFactory.getInstance( ModuleDao.class );
        List<Module> modules = moduleDao.getAll();

        for (Module module : modules) {
            addTreeItem( treeTable, module );
        }
    }


    private static void addTreeItem( TreeTable treeTable, Module module ) {

        String parentId = String.format( PARENT_PREFIX + "%s-%s", module.getGroupId(), module.getArtifactId() );
        treeTable.addItem( new Object[] { module.getGroupId(), module.getArtifactId() }, parentId );
        treeTable.addItem( new Object[] { module.getVersion(), "" }, module.getId() );

        treeTable.setParent( module.getId(), parentId );
    }
}
