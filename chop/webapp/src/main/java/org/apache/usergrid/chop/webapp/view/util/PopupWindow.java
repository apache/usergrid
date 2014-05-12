package org.apache.usergrid.chop.webapp.view.util;


import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Window;


public class PopupWindow extends Window {


    protected PopupWindow( String caption ) {
        init( caption );
        addItems();
    }


    private void init( String caption ) {
        setCaption( caption );
        setModal( true );
        setResizable( false );
        setWidth( "300px" );
        setHeight( "500px" );
    }


    private void addItems() {
        AbsoluteLayout mainLayout = addMainLayout();
        addCloseButton( mainLayout );
        addItems(mainLayout);
    }


    private void addCloseButton(AbsoluteLayout mainLayout) {

        Button closeButton = new Button( "Close" );

        closeButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                close();
            }
        } );

        mainLayout.addComponent( closeButton, "left: 220px; top: 425px;" );
    }


    private AbsoluteLayout addMainLayout() {

        AbsoluteLayout absoluteLayout = new AbsoluteLayout();
        absoluteLayout.setSizeFull();
        setContent( absoluteLayout );

        return absoluteLayout;
    }

    protected void addItems(AbsoluteLayout absoluteLayout) {

    }

}
