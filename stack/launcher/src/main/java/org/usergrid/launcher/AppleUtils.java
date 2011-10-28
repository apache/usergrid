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
