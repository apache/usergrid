package org.apache.usergrid.chop.webapp.view.user;


import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;


public class UserListWindow extends Window {

    public UserListWindow() {
        init();
        initContent();
    }


    private void init() {
        setCaption( "Users" );
        setModal( true );
        setResizable( false );
        setWidth( "300px" );
        setHeight( "500px" );
    }


    private void initContent() {

        AbsoluteLayout absoluteLayout = new AbsoluteLayout();
        absoluteLayout.setSizeFull();

        ListSelect list = new ListSelect();
        list.setWidth( "100%" );
        list.setHeight( "420px" );
        list.setNullSelectionAllowed( false );
        list.setImmediate( true );
        list.addItem( "User1" );
        list.addItem( "User2" );
        list.addItem( "User3" );
        list.addItem( "User4" );
        list.addItem( "User5" );

        absoluteLayout.addComponent( list, "left: 0px; top: 0px;" );
        absoluteLayout.addComponent( new Button( "Create" ), "left: 10px; top: 427px;" );


        Button closeButton = new Button( "Close" );
        closeButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                close();
            }
        } );

        absoluteLayout.addComponent( closeButton, "left: 220px; top: 427px;" );

        setContent( absoluteLayout );
    }
}
