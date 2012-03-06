/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.launcher;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class AppleUtils {

	public static void initMacApp() {
		com.apple.eawt.Application macApp = com.apple.eawt.Application
				.getApplication();
		macApp.setDockIconImage(new ImageIcon(App.class
				.getResource("dock_icon.png")).getImage());

		macApp.setAboutHandler(new com.apple.eawt.AboutHandler() {
			@Override
			public void handleAbout(com.apple.eawt.AppEvent.AboutEvent evt) {
				JOptionPane
						.showMessageDialog(
								null,
								"Usergrid Standalone Server Launcher\nCopyright 2011 Ed Anuff & Usergrid",
								"About Usergrid Launcher",
								JOptionPane.INFORMATION_MESSAGE);
			}
		});

	}
}
