/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.launcher;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {

    private static final Logger logger = LoggerFactory.getLogger( App.class );

    // TODO: host portal on Apache infrastructure, or build portal into launcher
    private static final String GH_PORTAL_URL = 
        "http://usergrid.apache.org/v101-portal-demo";

    public static boolean MAC_OS_X = 
        ( System.getProperty( "os.name" ).toLowerCase().startsWith( "mac os x" ) );

    LogViewerFrame logViewer = null;
    LauncherFrame launcher = null;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    boolean initializeDatabaseOnStart = true;
    boolean startDatabaseWithServer = true;
    Preferences prefs;
    String adminUserEmail = null;
    boolean autoLogin = true;


    public App() {
        /*
         * super("Launcher"); addComponentsToPane(); pack(); setVisible(true);
		 */
    }


    public static void main( String[] args ) {
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );
        System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "Usergrid Launcher" );
        System.setProperty( "apple.awt.antialiasing", "true" );
        System.setProperty( "apple.awt.textantialiasing", "true" );
        System.setProperty( "apple.awt.graphics.UseQuartz", "true" );
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }

        if ( MAC_OS_X ) {
            AppleUtils.initMacApp();
        }

        App app = new App();
        app.launch();
    }


    public void launch() {
        loadPrefs();

        try {
            logViewer = new LogViewerFrame( this );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }

        launcher = new LauncherFrame( this );

        logger.info( "App started" );
        // org.apache.usergrid.standalone.Server.main(new String[0]);
    }


    public static ArrayNode getJsonArray( Set<String> strings ) {
        ArrayNode node = JsonNodeFactory.instance.arrayNode();
        for ( String string : strings ) {
            node.add( string );
        }
        return node;
    }


    public void storeUrlsInPreferences( Set<String> urls ) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            prefs.put( "urlList", mapper.writeValueAsString( getJsonArray( urls ) ) );
        }
        catch ( Exception e ) {
        }
    }


    public void storeUrlsInPreferences( String[] urls ) {
        storeUrlsInPreferences( new LinkedHashSet<String>( Arrays.asList( urls ) ) );
    }


    public Set<String> getUrlSetFromPreferences() {
        Set<String> urls = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
        urls.add( GH_PORTAL_URL );
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = prefs.get( "urlList", null );
        }
        catch ( Exception e ) {
        }
        if ( json == null ) {
            return urls;
        }
        List<String> strings = null;
        try {
            strings = mapper.readValue( json, new TypeReference<List<String>>() {} );
        }
        catch ( Exception e ) {
        }
        if ( strings == null ) {
            return urls;
        }
        urls = new LinkedHashSet<String>( strings );
        urls.addAll( strings );
        return urls;
    }


    public String[] getUrlsFromPreferences() {
        Set<String> urls = getUrlSetFromPreferences();
        return urls.toArray( new String[urls.size()] );
    }


    Server server = null;


    public Server getServer() {
        if ( server == null ) {
            synchronized ( this ) {
                if ( server == null ) {
                    server = new Server();
                    server.setDaemon( false );
                }
            }
        }
        return server;
    }


    public void startServer() {
        executor.execute( new Runnable() {
            @Override
            public void run() {
                if ( !getServer().isRunning() ) {
                    launcher.setStatusYellow();
                    getServer().setInitializeDatabaseOnStart( initializeDatabaseOnStart );
                    getServer().setStartDatabaseWithServer( startDatabaseWithServer );
                    getServer().startServer();
                    launcher.setStatusGreen();
                }
            }
        } );
    }


    public synchronized void stopServer() {
        executor.execute( new Runnable() {
            @Override
            public void run() {
                if ( getServer().isRunning() ) {
                    getServer().stopServer();
                    launcher.setStatusRed();
                }
            }
        } );
    }


    public LogViewerFrame getLogViewer() {
        return logViewer;
    }


    public LauncherFrame getLauncher() {
        return launcher;
    }


    public void showLogView() {
        logViewer.setVisible( true );
    }


    public boolean isInitializeDatabaseOnStart() {
        return initializeDatabaseOnStart;
    }


    public void setInitializeDatabaseOnStart( boolean initializeDatabaseOnStart ) {
        this.initializeDatabaseOnStart = initializeDatabaseOnStart;
        prefs.putBoolean( "initializeDatabaseOnStart", initializeDatabaseOnStart );
    }


    public boolean isStartDatabaseWithServer() {
        return startDatabaseWithServer;
    }


    public void setStartDatabaseWithServer( boolean startDatabaseWithServer ) {
        this.startDatabaseWithServer = startDatabaseWithServer;
        prefs.putBoolean( "startDatabaseWithServer", startDatabaseWithServer );
    }


    public String getAdminUserEmail() {
        return adminUserEmail;
    }


    public void setAdminUserEmail( String adminUserEmail ) {
        this.adminUserEmail = adminUserEmail;
        prefs.put( "adminUserEmail", adminUserEmail );
    }


    public boolean isAutoLogin() {
        return autoLogin;
    }


    public void setAutoLogin( boolean autoLogin ) {
        this.autoLogin = autoLogin;
        prefs.putBoolean( "autoLogin", autoLogin );
    }


    public String getAccessToken() {
        return server.getAccessTokenForAdminUser( adminUserEmail );
    }


    public UUID getAdminUUID() {
        return server.getAdminUUID( adminUserEmail );
    }


    public boolean serverIsStarted() {
        return ( server != null ) && server.isRunning();
    }


    public void loadPrefs() {
        prefs = Preferences.userNodeForPackage( org.apache.usergrid.launcher.App.class );
        initializeDatabaseOnStart = prefs.getBoolean( "initializeDatabaseOnStart", true );
        startDatabaseWithServer = prefs.getBoolean( "startDatabaseWithServer", true );
        adminUserEmail = prefs.get( "adminUserEmail", "test@usergrid.com" );
        autoLogin = prefs.getBoolean( "autoLogin", true );
    }
}
