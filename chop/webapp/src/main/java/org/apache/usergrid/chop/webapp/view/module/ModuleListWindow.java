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
