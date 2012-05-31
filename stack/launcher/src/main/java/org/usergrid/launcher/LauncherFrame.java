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

import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class LauncherFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	enum Status {
		GREEN, RED, YELLOW
	}

	Status status = Status.RED;

	App app;

	ImageIcon start_icon = createImageIcon("start.png", "Start");
	ImageIcon start_active_icon = createImageIcon("start_active.png", "Start");
	JButton start_button;

	ImageIcon stop_icon = createImageIcon("stop.png", "Stop");
	ImageIcon stop_active_icon = createImageIcon("stop_active.png", "Start");
	JButton stop_button;

	ImageIcon log_viewer_icon = createImageIcon("log_viewer.png", "Log");
	JButton log_viewer_button;

	ImageIcon usergrid_admin_icon = createImageIcon("web_browser.png", "Admin");
	JButton usergrid_admin_button;

	JCheckBox start_database_checkbox;
	JCheckBox init_database_checkbox;
	JComboBox urlList;

	ImageIcon status_green = createImageIcon("status_green.png", "Green");
	ImageIcon status_yellow = createImageIcon("status_yellow.png", "Green");
	ImageIcon status_red = createImageIcon("status_red.png", "Green");
	ImageIcon status_off = createImageIcon("status_off.png", "Green");
	JLabel status_label;
	Timer status_timer;

	JCheckBox auto_login_checkbox;
	JTextField auto_login_email;

	public LauncherFrame(App app) {
		super("Usergrid Launcher");

		this.app = app;

		// getRootPane().putClientProperty("apple.awt.brushMetalLook",
		// Boolean.TRUE);
		// getRootPane().putClientProperty("apple.awt.antialiasing",
		// Boolean.TRUE);
		addComponentsToPane();
		pack();
		setBackground(new Color(196, 196, 196));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setVisible(true);
		setResizable(false);
	}

	public void addComponentsToPane() {
		Container pane = getContentPane();
		pane.setLayout(new GridBagLayout());

		GridBagConstraints c;

		JToolBar toolBar = new JToolBar("Toolbar");
		toolBar.setBackground(new Color(128, 128, 128));
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		toolBar.setMargin(new Insets(8, 16, 8, 8));
		toolBar.setBorder(new EmptyBorder(new Insets(8, 16, 8, 8)));

		status_label = new JLabel(status_red);
		status_label.setPreferredSize(new Dimension(24, 64));
		toolBar.add(status_label);
		status_timer = new Timer(750, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (status == Status.YELLOW) {
					if (status_label.getIcon() == status_yellow) {
						status_label.setIcon(status_off);
					} else {
						status_label.setIcon(status_yellow);
					}
				}
			}
		});
		status_timer.start();

		toolBar.addSeparator(new Dimension(16, 0));

		start_button = new JButton(start_active_icon);
		initButton(start_button);
		toolBar.add(start_button);
		start_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				start_button.setIcon(start_icon);
				stop_button.setIcon(stop_active_icon);
				app.startServer();
			}
		});

		toolBar.addSeparator(new Dimension(8, 0));

		stop_button = new JButton(stop_icon);
		initButton(stop_button);
		toolBar.add(stop_button);
		stop_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				start_button.setIcon(start_active_icon);
				stop_button.setIcon(stop_icon);
				app.stopServer();
			}
		});

		toolBar.addSeparator(new Dimension(8, 0));

		log_viewer_button = new JButton(log_viewer_icon);
		initButton(log_viewer_button);
		toolBar.add(log_viewer_button);
		log_viewer_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				app.showLogView();
			}
		});

		toolBar.addSeparator(new Dimension(8, 0));

		usergrid_admin_button = new JButton(usergrid_admin_icon);
		initButton(usergrid_admin_button);
		toolBar.add(usergrid_admin_button);
		usergrid_admin_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (app.serverIsStarted() && (status == Status.GREEN)) {
					storeAdminUrls();
					storeAdminEmail();
					try {
						Desktop.getDesktop().browse(getAdminURI());
					} catch (Exception e) {
					}
				} else {
					JOptionPane
							.showMessageDialog(
									null,
									"Server must be started before opening Admin Console.\n"
											+ "Please start server and wait for the status to turn green.",
									"Warning", JOptionPane.WARNING_MESSAGE);
				}

			}
		});

		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		add(toolBar, c);

		start_database_checkbox = new JCheckBox("Start Database With Server*");
		c = new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(16, 16, 8, 16), 0, 0);
		start_database_checkbox.setSelected(app.isStartDatabaseWithServer());
		start_database_checkbox.setFont(new Font("Arial", Font.BOLD, 18));
		pane.add(start_database_checkbox, c);
		start_database_checkbox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent change) {
				app.setStartDatabaseWithServer(start_database_checkbox
						.isSelected());
			}
		});

		init_database_checkbox = new JCheckBox("Initialize Database on Start*");
		c = new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(16, 16, 8, 16), 0, 0);
		init_database_checkbox.setSelected(app.isInitializeDatabaseOnStart());
		init_database_checkbox.setFont(new Font("Arial", Font.BOLD, 18));
		pane.add(init_database_checkbox, c);
		init_database_checkbox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent change) {
				app.setInitializeDatabaseOnStart(init_database_checkbox
						.isSelected());
			}
		});

		JLabel label = new JLabel("Console URL:");
		c = new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(16, 24, 8, 0), 0, 0);
		label.setFont(new Font("Arial", Font.BOLD, 18));
		pane.add(label, c);

		String[] urls = app.getUrlsFromPreferences();
		urlList = new JComboBox(urls);
		urlList.setEditable(true);
		c = new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
						16, 0, 8, 16), 0, 0);
		urlList.setFont(new Font("Arial", Font.BOLD, 18));

		urlList.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXX");

		ComboBoxEditor editor = urlList.getEditor();
		JTextField textField = (JTextField) editor.getEditorComponent();
		textField.setColumns(20);

		setPreferredWidth(textField, 350);
		setMaxWidth(textField, 350);

		setPreferredWidth(urlList, 350);
		setMaxWidth(urlList, 350);

		pane.add(urlList, c);

		auto_login_checkbox = new JCheckBox("Auto-login as:");
		c = new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(16, 16, 8, 0), 0, 0);
		auto_login_checkbox.setSelected(app.isAutoLogin());
		auto_login_checkbox.setFont(new Font("Arial", Font.BOLD, 18));
		pane.add(auto_login_checkbox, c);
		auto_login_checkbox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent change) {
				app.setAutoLogin(auto_login_checkbox.isSelected());
			}
		});

		auto_login_email = new JTextField(app.getAdminUserEmail());
		c = new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(16, 0, 8, 16), 0, 0);
		auto_login_email.setFont(new Font("Arial", Font.BOLD, 18));
		pane.add(auto_login_email, c);

		label = new JLabel(
				"* Database can only be started or initialized once per app launch");
		c = new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(16, 16, 16, 0), 0, 0);
		label.setForeground(Color.RED);
		label.setFont(new Font("Arial", Font.BOLD, 12));
		pane.add(label, c);

		List<Image> icons = new ArrayList<Image>(4);
		icons.add(new ImageIcon(getClass().getClassLoader().getResource(
				"org/usergrid/launcher/icon_16.png")).getImage());
		icons.add(new ImageIcon(getClass().getClassLoader().getResource(
				"org/usergrid/launcher/icon_32.png")).getImage());
		icons.add(new ImageIcon(getClass().getClassLoader().getResource(
				"org/usergrid/launcher/icon_64.png")).getImage());
		icons.add(new ImageIcon(getClass().getClassLoader().getResource(
				"org/usergrid/launcher/icon_256.png")).getImage());
		setIconImages(icons);
	}

	public void setPreferredWidth(JComponent jc, int width) {
		Dimension max = jc.getPreferredSize();
		max.width = width;
		jc.setPreferredSize(max);
	}

	public void setMaxWidth(JComponent jc, int width) {
		Dimension max = jc.getMaximumSize();
		max.width = width;
		jc.setMaximumSize(max);
	}

	public void initButton(JButton button) {
		button.setPreferredSize(new Dimension(64, 64));
		button.setMargin(new Insets(8, 8, 8, 8));
		button.setOpaque(false);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	}

	public void setStatusRed() {
		status = Status.RED;
		status_label.setIcon(status_red);
	}

	public void setStatusGreen() {
		status = Status.GREEN;
		status_label.setIcon(status_green);
	}

	public void setStatusYellow() {
		status = Status.YELLOW;
		status_label.setIcon(status_yellow);
	}

	public URI getAdminURI() throws URISyntaxException,
			UnsupportedEncodingException {
		String url = urlList.getSelectedItem().toString();
		if (url.contains("?")) {
			url += "&";
		} else {
			url += "?";
		}
		url += "api_url=" + URLEncoder.encode("http://localhost:8080", "UTF-8");
		if (app.isAutoLogin()) {
			String access_token = app.getAccessToken();
			UUID adminId = app.getAdminUUID();
			
			if (access_token != null) {
				url += "&admin_email="
						+ URLEncoder.encode(app.getAdminUserEmail(), "UTF-8");
				url += "&access_token=" + access_token;
			}
			
			if(adminId != null){
			    url += "&uuid="+adminId;
			}
		}
		return new URI(url);
	}

	public void storeAdminUrls() {
		Set<String> urls = app.getUrlSetFromPreferences();
		urls.add(urlList.getSelectedItem().toString());
		app.storeUrlsInPreferences(urls);
	}

	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	public void storeAdminEmail() {
		app.setAdminUserEmail(auto_login_email.getText());
	}

	public static class LauncherToolBar extends JToolBar {

		public LauncherToolBar() {
			super();
		}

		public LauncherToolBar(int orientation) {
			super(orientation);
		}

		public LauncherToolBar(String name, int orientation) {
			super(name, orientation);
		}

		public LauncherToolBar(String name) {
			super(name);
		}

		private static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent(Graphics g) {
			// Create the 2D copy
			Graphics2D g2 = (Graphics2D) g.create();

			// Apply vertical gradient
			g2.setPaint(new GradientPaint(0, 0, Color.WHITE, 0, getHeight(),
					Color.BLUE));
			g2.fillRect(0, 0, getWidth(), getHeight());

			// Dipose of copy
			g2.dispose();
		}

	}
}
