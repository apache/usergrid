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
package org.usergrid.mongo;

import static org.usergrid.utils.MapUtils.entry;
import static org.usergrid.utils.MapUtils.map;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.ManagementService;
import org.usergrid.management.UserInfo;
import org.usergrid.mongo.commands.MongoCommand;
import org.usergrid.mongo.protocol.Message;
import org.usergrid.mongo.protocol.OpQuery;
import org.usergrid.mongo.protocol.OpReply;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Schema;
import org.usergrid.security.shiro.PrincipalCredentialsToken;
import org.usergrid.security.shiro.utils.SubjectUtils;
import org.usergrid.services.ServiceManagerFactory;
import org.usergrid.utils.MapUtils;

public class MongoChannelHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(MongoChannelHandler.class);

	private final EntityManagerFactory emf;
	private final ServiceManagerFactory smf;
	private final ManagementService management;
	private final SessionsSecurityManager securityManager;

	Subject subject = null;

	public MongoChannelHandler(EntityManagerFactory emf,
			ServiceManagerFactory smf, ManagementService management,
			SessionsSecurityManager securityManager) {
		super();

		logger.info("Starting new client connection...");
		this.emf = emf;
		this.smf = smf;
		this.management = management;
		this.securityManager = securityManager;

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

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

		ThreadState threadState = null;
		if (subject != null) {
			threadState = new SubjectThreadState(subject);
			threadState.bind();
			// logger.info("Security subject bound to thread");
		}

		try {

			Message message = null;
			if (e.getMessage() instanceof Message) {
				message = (Message) e.getMessage();
			}

			if (message != null) {
				logger.info(">>> " + message.toString());
				OpReply reply = handleMessage(ctx, e, message);
				logger.info("<<< " + reply.toString() + "\n");
				e.getChannel().write(reply);
			}

		} finally {
			if (threadState != null) {
				threadState.clear();
				// logger.info("Security subject unbound from thread");
			}
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.warn("Unexpected exception from downstream.", e.getCause());
		e.getChannel().close();
	}

	public OpReply handleMessage(ChannelHandlerContext ctx, MessageEvent e,
			Message message) {

		Subject currentUser = SubjectUtils.getSubject();

		if (message instanceof OpQuery) {
			OpQuery q = (OpQuery) message;
			String collectionName = q.getCollectionName();
			if ("$cmd".equals(collectionName)) {
				@SuppressWarnings("unchecked")
				String commandName = (String) MapUtils.getFirstKey(q.getQuery()
						.toMap());
				if ("authenticate".equals(commandName)) {
					return handleAuthenticate(ctx, e, (OpQuery) message,
							q.getDatabaseName());
				} else if ("getnonce".equals(commandName)) {
					return handleGetnonce(ctx, e, (OpQuery) message);
				}
				if (!currentUser.isAuthenticated()) {
					return handleUnauthorizedCommand(ctx, e, (OpQuery) message);
				}
				MongoCommand command = MongoCommand.getCommand(commandName);
				if (command != null) {
					return command.execute(this, ctx, e, (OpQuery) message);
				} else {
					logger.info("No command for " + commandName);
				}
			} else {
				if (!currentUser.isAuthenticated()) {
					return handleUnauthorizedQuery(ctx, e, (OpQuery) message);
				}
				if ("system.applications".equals(collectionName)) {
					return handleListCollections(ctx, e, (OpQuery) message,
							q.getDatabaseName());
				} else if ("system.users".equals(collectionName)) {
					return handleListUsers(ctx, e, (OpQuery) message,
							q.getDatabaseName());
				} else {
					return handleQuery(ctx, e, (OpQuery) message,
							q.getDatabaseName(), collectionName);
				}
			}
		}

		OpReply reply = new OpReply(message);
		return reply;
	}

	public OpReply handleGetnonce(ChannelHandlerContext ctx, MessageEvent e,
			OpQuery opQuery) {
		String nonce = String.format("%04x", (new Random()).nextLong());
		OpReply reply = new OpReply(opQuery);
		reply.addDocument(map(entry("nonce", nonce), entry("ok", 1.0)));
		return reply;
	}

	public OpReply handleAuthenticate(ChannelHandlerContext ctx,
			MessageEvent e, OpQuery opQuery, String databaseName) {
		logger.info("Authenticating for database " + databaseName + "... ");
		String name = (String) opQuery.getQuery().get("user");
		String nonce = (String) opQuery.getQuery().get("nonce");
		String key = (String) opQuery.getQuery().get("key");

		UserInfo user = null;
		try {
			user = management.verifyMongoCredentials(name, nonce, key);
		} catch (Exception e1) {
			return handleAuthFails(ctx, e, opQuery);
		}
		if (user == null) {
			return handleAuthFails(ctx, e, opQuery);
		}

		PrincipalCredentialsToken token = PrincipalCredentialsToken
				.getFromAdminUserInfoAndPassword(user, key);
		Subject subject = SubjectUtils.getSubject();

		try {
			subject.login(token);
		} catch (AuthenticationException e2) {
			return handleAuthFails(ctx, e, opQuery);
		}

		OpReply reply = new OpReply(opQuery);
		reply.addDocument(map("ok", 1.0));
		return reply;
	}

	public OpReply handleUnauthorizedCommand(ChannelHandlerContext ctx,
			MessageEvent e, OpQuery opQuery) {
		// { "assertion" : "unauthorized db:admin lock type:-1 client:127.0.0.1"
		// , "assertionCode" : 10057 , "errmsg" : "db assertion failure" , "ok"
		// : 0.0}
		OpReply reply = new OpReply(opQuery);
		reply.addDocument(map(
				entry("assertion",
						"unauthorized db:"
								+ opQuery.getDatabaseName()
								+ " lock type:-1 client:"
								+ ((InetSocketAddress) e.getRemoteAddress())
										.getAddress().getHostAddress()),
				entry("assertionCode", 10057),
				entry("errmsg", "db assertion failure"), entry("ok", 0.0)));
		return reply;
	}

	public OpReply handleUnauthorizedQuery(ChannelHandlerContext ctx,
			MessageEvent e, OpQuery opQuery) {
		// { "$err" : "unauthorized db:test lock type:-1 client:127.0.0.1" ,
		// "code" : 10057}
		OpReply reply = new OpReply(opQuery);
		reply.addDocument(map(
				entry("$err",
						"unauthorized db:"
								+ opQuery.getDatabaseName()
								+ " lock type:-1 client:"
								+ ((InetSocketAddress) e.getRemoteAddress())
										.getAddress().getHostAddress()),
				entry("code", 10057)));
		return reply;
	}

	public OpReply handleAuthFails(ChannelHandlerContext ctx, MessageEvent e,
			OpQuery opQuery) {
		// { "errmsg" : "auth fails" , "ok" : 0.0}
		OpReply reply = new OpReply(opQuery);
		reply.addDocument(map(entry("errmsg", "auth fails"), entry("ok", 0.0)));
		return reply;
	}

	public OpReply handleListCollections(ChannelHandlerContext ctx,
			MessageEvent e, OpQuery opQuery, String databaseName) {
		logger.info("Handling list collections for database " + databaseName
				+ "... ");

		ApplicationInfo application = SubjectUtils.getApplication(Identifier
				.fromName(databaseName));
		if (application == null) {
			OpReply reply = new OpReply(opQuery);
			return reply;
		}
		EntityManager em = emf.getEntityManager(application.getId());
		OpReply reply = new OpReply(opQuery);
		try {
			Set<String> collections = em.getApplicationCollections();
			for (String colName : collections) {
				if (Schema.isAssociatedEntityType(colName)) {
					continue;
				}
				reply.addDocument(map("name", databaseName + "." + colName));
				reply.addDocument(map("name", databaseName + "." + colName
						+ ".$_id_"));
			}
			// reply.addDocument(map("name", databaseName + ".system.indexes"));
		} catch (Exception ex) {
			logger.error("Unable to retrieve collections", ex);
		}
		return reply;
	}

	public OpReply handleListUsers(ChannelHandlerContext ctx, MessageEvent e,
			OpQuery opQuery, String databaseName) {
		logger.info("Handling list users for database " + databaseName + "... ");

		OpReply reply = new OpReply(opQuery);
		return reply;
	}

	public OpReply handleQuery(ChannelHandlerContext ctx, MessageEvent e,
			OpQuery opQuery, String databaseName, String collectionName) {
		logger.info("Handling a query... ");
		ApplicationInfo application = SubjectUtils.getApplication(Identifier
				.fromName(databaseName));
		if (application == null) {
			OpReply reply = new OpReply(opQuery);
			return reply;
		}
		int count = opQuery.getNumberToReturn();
		if (count <= 0) {
			count = 30;
		}
		EntityManager em = emf.getEntityManager(application.getId());
		OpReply reply = new OpReply(opQuery);
		try {
			Results results = em.getCollection(em.getApplicationRef(),
					collectionName, null, count, Results.Level.ALL_PROPERTIES,
					false);
			if (!results.isEmpty()) {
				for (Entity entity : results.getEntities()) {
					reply.addDocument(map(
							entry("_id", entity.getUuid()),
							entity,
							entry(Schema.PROPERTY_UUID, entity.getUuid()
									.toString())));
				}
			}
		} catch (Exception ex) {
			logger.error("Unable to retrieve collections", ex);
		}
		return reply;
	}

}
