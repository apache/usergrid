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


import javax.swing.ImageIcon;
//import javax.swing.JOptionPane;


public class AppleUtils {

    public static void initMacApp() {
        com.apple.eawt.Application macApp = com.apple.eawt.Application.getApplication();
        macApp.setDockIconImage( new ImageIcon( App.class.getResource( "dock_icon.png" ) ).getImage() );

        // commented out to allow launcher to compile with the old AppleJavaExcentions 1.4
        // (because that is the newest version available via Maven)

//        macApp.setAboutHandler( new com.apple.eawt.AboutHandler() {
//            @Override
//            public void handleAbout( com.apple.eawt.AppEvent.AboutEvent evt ) {
//                JOptionPane.showMessageDialog( null,
//                        "Apache Usergrid Standalone Server Launcher\nApache Software Foundation",
//                        "About Apache Usergrid Launcher", JOptionPane.INFORMATION_MESSAGE );
//            }
//        } );
    }
}
