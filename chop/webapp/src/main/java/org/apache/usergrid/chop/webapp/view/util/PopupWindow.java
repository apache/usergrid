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
