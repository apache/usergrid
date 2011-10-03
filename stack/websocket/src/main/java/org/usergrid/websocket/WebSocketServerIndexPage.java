/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
 ******************************************************************************/
/*
 * Copyright 2010 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.usergrid.websocket;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;

/**
 * Generates the demo HTML page which is served at http://localhost:8080/
 * 
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * 
 * @version $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $
 */
public class WebSocketServerIndexPage {

	private static final String NEWLINE = "\r\n";

	public static ChannelBuffer getContent(String webSocketLocation) {
		return ChannelBuffers
				.copiedBuffer(
						"<html><head><title>Web Socket Test</title></head>"
								+ NEWLINE
								+ "<body>"
								+ NEWLINE
								+ "<script type=\"text/javascript\">"
								+ NEWLINE
								+ "var socket;"
								+ NEWLINE
								+ "if (window.WebSocket) {"
								+ NEWLINE
								+ "  socket = new WebSocket(\""
								+ webSocketLocation
								+ "/00000000-0000-0000-0000-000000000001/users/00000000-0000-0000-0000-000000000002?a=1\");"
								+ NEWLINE
								+ "  socket.onmessage = function(event) { alert(event.data); };"
								+ NEWLINE
								+ "  socket.onopen = function(event) { alert(\"Web Socket opened!\"); };"
								+ NEWLINE
								+ "  socket.onclose = function(event) { alert(\"Web Socket closed.\"); };"
								+ NEWLINE
								+ "} else {"
								+ NEWLINE
								+ "  alert(\"Your browser does not support Web Socket.\");"
								+ NEWLINE
								+ "}"
								+ NEWLINE
								+ ""
								+ NEWLINE
								+ "function send(message) {"
								+ NEWLINE
								+ "  if (!window.WebSocket) { return; }"
								+ NEWLINE
								+ "  if (socket.readyState == 1) {"
								+ NEWLINE
								+ "    socket.send(message);"
								+ NEWLINE
								+ "  } else {"
								+ NEWLINE
								+ "    alert(\"The socket is not open.\");"
								+ NEWLINE
								+ "  }"
								+ NEWLINE
								+ "}"
								+ NEWLINE
								+ "</script>"
								+ NEWLINE
								+ "<form onsubmit=\"return false;\">"
								+ NEWLINE
								+ "<input type=\"text\" name=\"message\" value=\"Hello, World!\"/>"
								+ "<input type=\"button\" value=\"Send Web Socket Data\" onclick=\"send(this.form.message.value)\" />"
								+ NEWLINE + "</form>" + NEWLINE + "</body>"
								+ NEWLINE + "</html>" + NEWLINE,
						CharsetUtil.US_ASCII);
	}
}
