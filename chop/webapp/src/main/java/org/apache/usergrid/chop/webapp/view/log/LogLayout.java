/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.chop.webapp.view.log;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.vaadin.ui.Button;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogLayout extends VerticalLayout {

    private final static Logger LOG = LoggerFactory.getLogger( LogLayout.class );

    private TextArea logArea;
    private RandomAccessFile r;
    private File file;


    public LogLayout() {
        initializeUIComponents();
        this.setSizeFull();
    }


    private void initializeUIComponents() {
        addLogArea();
        addRefreshButton();
    }


    private void addLogArea() {
        logArea = new TextArea( "Coordinator Logs" );

        // TODO make this file point configurable
        file = new File( "/var/log/chop-webapp.log" );
        try {
            r = new RandomAccessFile( file, "r" );
        }
        catch ( FileNotFoundException e ) {
            LOG.error( "Error while accessing file {}: {}", file, e );
        }
        logArea.setHeight( "100%" );
        logArea.setWidth( "100%" );
        getApplicationLog();
        addComponent( logArea );
        this.setComponentAlignment( logArea, Alignment.TOP_CENTER );
        this.setExpandRatio( logArea, 0.95f );
    }


    private void getApplicationLog() {
        logArea.setReadOnly( false );

        try {
            String str = null;
            StringBuilder log = new StringBuilder();
            log.append( logArea.getValue() );
            while( ( str = r.readLine() ) != null ) {
                log.append( str );
                log.append( System.getProperty( "line.separator" ) );
            }
            r.seek( r.getFilePointer() );
            logArea.setValue( log.toString() );
            logArea.setCursorPosition( log.toString().length() - 1 );

        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        logArea.setReadOnly( true );
    }


    private void addRefreshButton()  {
        Button button = new Button( "Refresh" );
        button.setWidth("100px");
        button.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                loadData();
            }
        });
        addComponent( button );
        setComponentAlignment( button, Alignment.BOTTOM_CENTER );
        this.setExpandRatio( button, 0.05f );

    }


    private void loadData() {
        getApplicationLog();
    }

}
