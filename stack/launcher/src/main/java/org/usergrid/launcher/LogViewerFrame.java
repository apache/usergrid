package org.usergrid.launcher;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

public class LogViewerFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	JTextArea textArea = new JTextArea();

	App app;

	public LogViewerFrame(App app) throws IOException {
		super("Log");
		this.app = app;

		// Add a scrolling text area
		textArea.setEditable(false);
		textArea.setRows(20);
		textArea.setColumns(50);
		getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
		pack();
		// setLocationRelativeTo(app.getLauncher());
		setLocation(100, 100);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setVisible(false);

		Log4jAppender appender = new Log4jAppender();
		Logger.getRootLogger().addAppender(appender);

	}

	public void appendMessage(final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				textArea.append(message);
				try {
					textArea.setCaretPosition(textArea.getDocument()
							.getLength());
				} catch (Exception e) {
				}
			}
		});
	}

	public class Log4jAppender extends AppenderSkeleton {

		PatternLayout layout;

		public Log4jAppender() {
			layout = new PatternLayout(
					"[%d{dd-MMM-yyyy HH:mm:ss,SSS}][%p][%t] %l %m%n");
		}

		@Override
		public void close() {
		}

		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		protected void append(LoggingEvent loggingEvent) {
			appendMessage(layout.format(loggingEvent));
		}

	}

}