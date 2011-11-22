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
package org.usergrid.websocket;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.apache.commons.lang.StringUtils.split;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ORIGIN;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SEC_WEBSOCKET_KEY1;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SEC_WEBSOCKET_KEY2;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SEC_WEBSOCKET_LOCATION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SEC_WEBSOCKET_ORIGIN;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SEC_WEBSOCKET_PROTOCOL;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.WEBSOCKET_LOCATION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.WEBSOCKET_ORIGIN;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.WEBSOCKET_PROTOCOL;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.WEBSOCKET;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.subject.Subject;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.services.ServiceManagerFactory;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

public class WebSocketChannelHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(WebSocketChannelHandler.class);

	private final EntityManagerFactory emf;
	private final ServiceManagerFactory smf;
	private final ManagementService management;
	private final SessionsSecurityManager securityManager;
	private final boolean ssl;

	boolean websocket = false;

	Subject subject = null;

	// static ConcurrentHashMap<String, ChannelGroup> subscribers = new
	// ConcurrentHashMap<String, ChannelGroup>();

	static ConcurrentMap<String, ChannelGroup> subscribers = new MapMaker()
			.makeComputingMap(new Function<String, ChannelGroup>() {
				@Override
				public ChannelGroup apply(String key) {
					return new DefaultChannelGroup();
				}
			});

	List<String> subscriptions;

	public WebSocketChannelHandler(EntityManagerFactory emf,
			ServiceManagerFactory smf, ManagementService management,
			SessionsSecurityManager securityManager, boolean ssl) {
		super();

		this.emf = emf;
		this.smf = smf;
		this.management = management;
		this.securityManager = securityManager;
		this.ssl = ssl;

		if (securityManager != null) {
			subject = new Subject.Builder(securityManager).buildSubject();
		}
	}

	public EntityManagerFactory getEmf() {
		return emf;
	}

	public ServiceManagerFactory getSmf() {
		return smf;
	}

	public ManagementService getOrganizations() {
		return management;
	}

	public SessionsSecurityManager getSecurityManager() {
		return securityManager;
	}

	private String getWebSocketLocation(HttpRequest req) {
		String path = req.getUri();
		if (path.equals("/")) {
			path = null;
		}
		if (path != null) {
			path = removeEnd(path, "/");
		}
		String location = (ssl ? "wss://" : "ws://")
				+ req.getHeader(HttpHeaders.Names.HOST)
				+ (path != null ? path : "");
		logger.info(location);
		return location;
	}

	private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req,
			HttpResponse res) {
		// Generate an error page if response status code is not OK (200).
		if (res.getStatus().getCode() != 200) {
			res.setContent(ChannelBuffers.copiedBuffer(res.getStatus()
					.toString(), CharsetUtil.UTF_8));
			setContentLength(res, res.getContent().readableBytes());
		}

		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.getChannel().write(res);
		if (!isKeepAlive(req) || (res.getStatus().getCode() != 200)) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req,
			HttpResponseStatus status) {
		sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, status));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.warn("Unexpected exception from downstream.", e.getCause());
		e.getChannel().close();
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		super.channelDisconnected(ctx, e);
		if (websocket) {
			logger.info("Websocket disconnected");
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof HttpRequest) {
			handleHttpRequest(ctx, (HttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req)
			throws Exception {
		// Allow only GET methods.
		if (req.getMethod() != GET) {
			sendHttpResponse(ctx, req, FORBIDDEN);
			return;
		}

		boolean is_ws_request = Values.UPGRADE.equalsIgnoreCase(req
				.getHeader(CONNECTION))
				&& WEBSOCKET.equalsIgnoreCase(req.getHeader(Names.UPGRADE));

		// Send the demo page.
		if (!is_ws_request && req.getUri().equals("/")) {
			HttpResponse res = new DefaultHttpResponse(HTTP_1_1, OK);

			ChannelBuffer content = WebSocketServerIndexPage
					.getContent(getWebSocketLocation(req));

			res.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8");
			setContentLength(res, content.readableBytes());

			res.setContent(content);
			sendHttpResponse(ctx, req, res);

			return;

		} else if (is_ws_request) {
			// Serve the WebSocket handshake request.

			logger.info("Starting new websocket connection...");
			websocket = true;

			// Create the WebSocket handshake response.
			HttpResponse res = new DefaultHttpResponse(
					HTTP_1_1,
					new HttpResponseStatus(101, "Web Socket Protocol Handshake"));
			res.addHeader(Names.UPGRADE, WEBSOCKET);
			res.addHeader(CONNECTION, Values.UPGRADE);

			QueryStringDecoder qs = new QueryStringDecoder(req.getUri());
			String path = qs.getPath();
			logger.info(path);

			// Fill in the headers and contents depending on handshake method.
			if (req.containsHeader(SEC_WEBSOCKET_KEY1)
					&& req.containsHeader(SEC_WEBSOCKET_KEY2)) {

				String[] segments = split(path, '/');

				if (segments.length != 3) {
					logger.info("Wrong number of path segments, expected 3, found "
							+ segments.length);
					sendHttpResponse(ctx, req, FORBIDDEN);
					return;
				}

				String nsStr = segments[0];
				String collStr = segments[1];
				String idStr = segments[2];

				logger.info(nsStr + "/" + collStr + "/" + idStr);

				if (isEmpty(nsStr) || isEmpty(collStr) || isEmpty(idStr)) {
					sendHttpResponse(ctx, req, FORBIDDEN);
					return;
				}

				// New handshake method with a challenge:
				res.addHeader(SEC_WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
				res.addHeader(SEC_WEBSOCKET_LOCATION, getWebSocketLocation(req));
				String protocol = req.getHeader(SEC_WEBSOCKET_PROTOCOL);
				if (protocol != null) {
					res.addHeader(SEC_WEBSOCKET_PROTOCOL, protocol);
				}

				// Calculate the answer of the challenge.
				String key1 = req.getHeader(SEC_WEBSOCKET_KEY1);
				String key2 = req.getHeader(SEC_WEBSOCKET_KEY2);
				int a = (int) (Long.parseLong(key1.replaceAll("[^0-9]", "")) / key1
						.replaceAll("[^ ]", "").length());
				int b = (int) (Long.parseLong(key2.replaceAll("[^0-9]", "")) / key2
						.replaceAll("[^ ]", "").length());
				long c = req.getContent().readLong();
				ChannelBuffer input = ChannelBuffers.buffer(16);
				input.writeInt(a);
				input.writeInt(b);
				input.writeLong(c);
				ChannelBuffer output = ChannelBuffers
						.wrappedBuffer(MessageDigest.getInstance("MD5").digest(
								input.array()));
				res.setContent(output);
			} else {
				// Old handshake method with no challenge:
				res.addHeader(WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
				res.addHeader(WEBSOCKET_LOCATION, getWebSocketLocation(req));
				String protocol = req.getHeader(WEBSOCKET_PROTOCOL);
				if (protocol != null) {
					res.addHeader(WEBSOCKET_PROTOCOL, protocol);
				}
			}

			// Upgrade the connection and send the handshake response.
			ChannelPipeline p = ctx.getChannel().getPipeline();
			p.remove("aggregator");
			p.replace("decoder", "wsdecoder", new WebSocketFrameDecoder());

			ctx.getChannel().write(res);

			p.replace("encoder", "wsencoder", new WebSocketFrameEncoder());

			return;
		}

		// Send an error page otherwise.
		sendHttpResponse(ctx, req, FORBIDDEN);
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx,
			WebSocketFrame frame) {
		// Send the uppercased string back.
		ctx.getChannel().write(
				new DefaultWebSocketFrame(frame.getTextData().toUpperCase()));
	}

	// TODO Review this for concurrency safety
	// Note: subscriptions are added and removed relatively infrequently
	// during the lifecycle of a connection i.e. typical minimum lifespan
	// would be 10 seconds when someone opens an app, it connects, then
	// they close the app.

	public void addSubscription(String path, Channel channel) {
		ChannelGroup group = subscribers.get(path);
		synchronized (group) {
			group.add(channel);
		}
	}

	public void removeSubscription(String path, Channel channel) {
		ChannelGroup group = subscribers.get(path);
		synchronized (group) {
			group.remove(channel);
			if (group.isEmpty()) {
				subscribers.remove(path, group);
			}
		}
	}

	public ChannelGroup getSubscriptionGroup(String path) {
		return subscribers.get(path);
	}
}
