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

import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.TreeTable;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ModuleTreeBuilder {

    private static Logger LOG = LoggerFactory.getLogger( ModuleTreeBuilder.class );

    private static final String PARENT_PREFIX = "parent:";

    public static TreeTable getTree(ModuleSelectListener listener) {

        TreeTable treeTable = new TreeTable("Modules");
        treeTable.addContainerProperty("Group", String.class, "");
        treeTable.addContainerProperty("Artifact", String.class, "");
        treeTable.setSizeFull();
        treeTable.addItemClickListener(getItemClickListener(listener));

        addItems(treeTable);

        return treeTable;
    }

    private static ItemClickListener getItemClickListener(final ModuleSelectListener listener) {
        return new ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                onItemClick(event, listener);
            }
        };
    }

    private static void onItemClick(ItemClickEvent event, ModuleSelectListener listener) {

        String id = (String) event.getItemId();
        boolean isModuleVersion = !StringUtils.startsWith(id, PARENT_PREFIX);

        if (isModuleVersion) {
            listener.onModuleSelect(id);
//            listener.onModuleSelect("1414303914");
        }
    }

    private static void addItems(TreeTable treeTable) {

        ModuleDao moduleDao = InjectorFactory.getInstance(ModuleDao.class);
        List<Module> modules = moduleDao.getAll();

        for (Module module : modules) {
            addItem(treeTable, module);
        }
    }

    private static void addItem(TreeTable treeTable, Module module) {

        String parentId = String.format( PARENT_PREFIX + "%s-%s", module.getGroupId(), module.getArtifactId() );
        treeTable.addItem( new Object[] { module.getGroupId(), module.getArtifactId() }, parentId );
        treeTable.addItem( new Object[] { module.getVersion(), "" }, module.getId() );

        treeTable.setParent( module.getId(), parentId );
    }

}
