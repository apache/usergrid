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
package org.usergrid.mongo.protocol;

import static org.apache.commons.collections.MapUtils.getIntValue;
import static org.usergrid.utils.JsonUtils.toJsonMap;
import static org.usergrid.utils.MapUtils.entry;
import static org.usergrid.utils.MapUtils.map;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import org.antlr.runtime.ClassicToken;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.mongo.MongoChannelHandler;
import org.usergrid.mongo.commands.MongoCommand;
import org.usergrid.mongo.utils.BSONUtils;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.Query.SortDirection;
import org.usergrid.persistence.query.tree.AndOperand;
import org.usergrid.persistence.query.tree.Equal;
import org.usergrid.persistence.query.tree.GreaterThan;
import org.usergrid.persistence.query.tree.GreaterThanEqual;
import org.usergrid.persistence.query.tree.LessThan;
import org.usergrid.persistence.query.tree.LessThanEqual;
import org.usergrid.persistence.query.tree.Operand;
import org.usergrid.persistence.query.tree.OrOperand;
import org.usergrid.security.shiro.PrincipalCredentialsToken;
import org.usergrid.security.shiro.utils.SubjectUtils;
import org.usergrid.utils.MapUtils;

public class OpQuery extends OpCrud {

    private static final Logger logger = LoggerFactory.getLogger(OpQuery.class);

    int flags;
    int numberToSkip;
    int numberToReturn;
    BSONObject query;
    BSONObject returnFieldSelector;

    static Set<String> operators = new HashSet<String>();
    {
        operators.add("all");
        operators.add("and");
        operators.add("elemMatch");
        operators.add("exists");
        operators.add("gt");
        operators.add("gte");
        operators.add("in");
        operators.add("lt");
        operators.add("lte");
        operators.add("mod");
        operators.add("ne");
        operators.add("nin");
        operators.add("nor");
        operators.add("not");
        operators.add("or");
        operators.add("regex");
        operators.add("size");
        operators.add("type");
        operators.add("where");
    }

    public OpQuery() {
        opCode = OP_QUERY;
    }

    public Query toNativeQuery() {
        if (query == null) {
            return null;
        }

        BasicBSONObject query_expression = null;
        BasicBSONObject sort_order = null;

        Object o = query.get("$query");
        if (!(o instanceof BasicBSONObject)) {
            o = query.get("query");
        }
        if (o instanceof BasicBSONObject) {
            query_expression = (BasicBSONObject) o;
        }

        o = query.get("$orderby");
        if (!(o instanceof BasicBSONObject)) {
            o = query.get("orderby");
        }
        if (o instanceof BasicBSONObject) {
            sort_order = (BasicBSONObject) o;
        }

        if ((query_expression == null) && (query instanceof BasicBSONObject)) {
            query_expression = (BasicBSONObject) query;
            query_expression.removeField("$orderby");
            query_expression.removeField("$max");
            query_expression.removeField("$min");
        }

        if ((query_expression == null) && (sort_order == null)) {
            return null;
        }

        if (query_expression.size() == 0 && sort_order != null) {
            if (sort_order.size() == 0) {
                return null;
            }
            if ((sort_order.size() == 1) && sort_order.containsField("_id")) {
                return null;
            }
        }

        Query q = new Query();
        if (getNumberToReturn() > 0) {
            q.setLimit(getNumberToReturn());
        }

        if (query_expression != null) {
            Operand root = eval(query_expression);
            q.setRootOperand(root);
        }

        if (sort_order != null) {
            for (String sort : sort_order.keySet()) {
                if (!"_id".equals(sort)) {
                    int s = getIntValue(sort_order.toMap(), "_id", 1);
                    q.addSort(sort, s >= 0 ? SortDirection.ASCENDING
                            : SortDirection.DESCENDING);
                }
            }
        }

        return q;
    }

    private Operand eval(BSONObject exp) {
        Operand current = null;
        Object fieldValue = null;

        for (String field : exp.keySet()) {
            fieldValue = exp.get(field);

            if (field.startsWith("$")) {
                // same as OR with multiple values

                // same as OR with multiple values
                if ("$or".equals(field)) {
                    BasicBSONList values = (BasicBSONList) fieldValue;

                    int size = values.size();

                    Stack<Operand> expressions = new Stack<Operand>();

                    for (int i = 0; i < size; i++) {
                        expressions.push(eval((BSONObject) values.get(i)));
                    }

                    // we need to build a tree of expressions
                    while (expressions.size() > 1) {
                        OrOperand or = new OrOperand();
                        or.addChild(expressions.pop());
                        or.addChild(expressions.pop());
                        expressions.push(or);
                    }

                    current = expressions.pop();

                }

                else if ("$and".equals(field)) {

                    BasicBSONList values = (BasicBSONList) fieldValue;

                    int size = values.size();

                    Stack<Operand> expressions = new Stack<Operand>();

                    for (int i = 0; i < size; i++) {
                        expressions.push(eval((BSONObject) values.get(i)));
                    }

                    while (expressions.size() > 1) {
                        AndOperand and = new AndOperand();
                        and.addChild(expressions.pop());
                        and.addChild(expressions.pop());
                        expressions.push(and);
                    }

                    current = expressions.pop();
                }

            }
            // we have a nested object
            else if (fieldValue instanceof BSONObject) {
                current = handleOperand(field, (BSONObject) fieldValue);
            }

            else if (!field.equals("_id")) {
                Equal equality = new Equal(new ClassicToken(0, "="));
                equality.setProperty(field);
                equality.setLiteral(exp.get(field));

                current = equality;
            }
        }
        return current;
    }

    private Operand handleOperand(String sourceField, BSONObject exp) {

        Operand current = null;
        Object value = null;

        for (String field : exp.keySet()) {
            if (field.startsWith("$")) {
                if ("$gt".equals(field)) {
                    value = exp.get(field);

                    GreaterThan gt = new GreaterThan();
                    gt.setProperty(sourceField);
                    gt.setLiteral(value);

                    current = gt;
                } else if ("$gte".equals(field)) {
                    value = exp.get(field);

                    GreaterThanEqual gte = new GreaterThanEqual();
                    gte.setProperty(sourceField);
                    gte.setLiteral(exp.get(field));

                    current = gte;
                    // http://www.mongodb.org/display/DOCS/Advanced+Queries#AdvancedQueries-%3C%2C%3C%3D%2C%3E%2C%3E%3D
                    // greater than equals
                    // { "field" : { $gte: value } }
                } else if ("$lt".equals(field)) {
                    value = exp.get(field);

                    LessThan lt = new LessThan();
                    lt.setProperty(sourceField);
                    lt.setLiteral(value);

                    current = lt;
                } else if ("$lte".equals(field)) {
                    value = exp.get(field);

                    LessThanEqual lte = new LessThanEqual();
                    lte.setProperty(sourceField);
                    lte.setLiteral(value);

                    current = lte;
                } else if ("$in".equals(field)) {
                    value = exp.get(field);

                    BasicBSONList values = (BasicBSONList) value;

                    int size = values.size();

                    Stack<Operand> expressions = new Stack<Operand>();

                    for (int i = 0; i < size; i++) {
                        Equal equal = new Equal();
                        equal.setProperty(sourceField);
                        equal.setLiteral(values.get(i));

                        expressions.push(equal);
                    }

                    // we need to build a tree of expressions
                    while (expressions.size() > 1) {
                        OrOperand or = new OrOperand();
                        or.addChild(expressions.pop());
                        or.addChild(expressions.pop());
                        expressions.push(or);
                    }

                    current = expressions.pop();

                }

            }
        }

        return current;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getNumberToSkip() {
        return numberToSkip;
    }

    public void setNumberToSkip(int numberToSkip) {
        this.numberToSkip = numberToSkip;
    }

    public int getNumberToReturn() {
        return numberToReturn;
    }

    public void setNumberToReturn(int numberToReturn) {
        this.numberToReturn = numberToReturn;
    }

    public BSONObject getQuery() {
        return query;
    }

    public void setQuery(BSONObject query) {
        this.query = query;
    }

    public void setQuery(Map<?, ?> map) {
        query = new BasicBSONObject();
        query.putAll(map);
    }

    public BSONObject getReturnFieldSelector() {
        return returnFieldSelector;
    }

    public void setReturnFieldSelector(BSONObject returnFieldSelector) {
        this.returnFieldSelector = returnFieldSelector;
    }

    public void setReturnFieldSelector(Map<?, ?> map) {
        returnFieldSelector = new BasicBSONObject();
        returnFieldSelector.putAll(map);
    }

    @Override
    public void decode(ChannelBuffer buffer) throws IOException {
        super.decode(buffer);
        flags = buffer.readInt();
        fullCollectionName = readCString(buffer);
        numberToSkip = buffer.readInt();
        numberToReturn = buffer.readInt();
        query = BSONUtils.decoder().readObject(
                new ChannelBufferInputStream(buffer));
        if (buffer.readable()) {
            returnFieldSelector = BSONUtils.decoder().readObject(
                    new ChannelBufferInputStream(buffer));
        }
    }

    @Override
    public ChannelBuffer encode(ChannelBuffer buffer) {
        int l = 28; // 7 ints * 4 bytes

        ByteBuffer fullCollectionNameBytes = getCString(fullCollectionName);
        l += fullCollectionNameBytes.capacity();

        ByteBuffer queryBytes = encodeDocument(query);
        l += queryBytes.capacity();

        ByteBuffer returnFieldSelectorBytes = encodeDocument(returnFieldSelector);
        l += returnFieldSelectorBytes.capacity();

        messageLength = l;

        buffer = super.encode(buffer);

        buffer.writeInt(flags);

        buffer.writeBytes(fullCollectionNameBytes);

        buffer.writeInt(numberToSkip);
        buffer.writeInt(numberToReturn);

        buffer.writeBytes(queryBytes);

        buffer.writeBytes(returnFieldSelectorBytes);

        return buffer;
    }

    @Override
    public String toString() {
        return "OpQuery [flags=" + flags + ", fullCollectionName="
                + fullCollectionName + ", numberToSkip=" + numberToSkip
                + ", numberToReturn=" + numberToReturn + ", query=" + query
                + ", returnFieldSelector=" + returnFieldSelector + "]";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.mongo.protocol.OpCrud#doOp()
     */
    @Override
    public OpReply doOp(MongoChannelHandler handler, ChannelHandlerContext ctx,
            MessageEvent messageEvent) {

        Subject currentUser = SubjectUtils.getSubject();

        String collectionName = getCollectionName();
        
        if ("$cmd".equals(collectionName)) {
           
            @SuppressWarnings("unchecked")
            String commandName = (String) MapUtils.getFirstKey(getQuery().toMap());
            
            if ("authenticate".equals(commandName)) {
                return handleAuthenticate(handler, getDatabaseName());
            }
            
            if ("getnonce".equals(commandName)) {
                return handleGetnonce();
            }
            
            if (!currentUser.isAuthenticated()) {
                return handleUnauthorizedCommand(messageEvent);
            }

            MongoCommand command = MongoCommand.getCommand(commandName);

            if (command != null) {
                return command.execute(handler, ctx, messageEvent, this);
            } else {
                logger.info("No command for " + commandName);
            }
        }

        if (!currentUser.isAuthenticated()) {
            return handleUnauthorizedQuery(messageEvent);
        }
        
        if ("system.namespaces".equals(collectionName)) {
            return handleListCollections(handler, getDatabaseName());
        } 
        
        if ("system.users".equals(collectionName)) {
            return handleListUsers();
        }
        
        return handleQuery(handler);

    }

    private OpReply handleAuthenticate(MongoChannelHandler handler,
            String databaseName) {
        logger.info("Authenticating for database " + databaseName + "... ");
        String name = (String) query.get("user");
        String nonce = (String) query.get("nonce");
        String key = (String) query.get("key");

        UserInfo user = null;
        try {
            user = handler.getOrganizations().verifyMongoCredentials(name,
                    nonce, key);
        } catch (Exception e1) {
            return handleAuthFails(this);
        }
        if (user == null) {
            return handleAuthFails(this);
        }

        PrincipalCredentialsToken token = PrincipalCredentialsToken
                .getFromAdminUserInfoAndPassword(user, key);
        Subject subject = SubjectUtils.getSubject();

        try {
            subject.login(token);
        } catch (AuthenticationException e2) {
            return handleAuthFails(this);
        }

        OpReply reply = new OpReply(this);
        reply.addDocument(map("ok", 1.0));
        return reply;
    }

    private OpReply handleGetnonce() {
        String nonce = String.format("%04x", (new Random()).nextLong());
        OpReply reply = new OpReply(this);
        reply.addDocument(map(entry("nonce", nonce), entry("ok", 1.0)));
        return reply;
    }

    private OpReply handleUnauthorizedCommand(MessageEvent e) {
        // { "assertion" : "unauthorized db:admin lock type:-1 client:127.0.0.1"
        // , "assertionCode" : 10057 , "errmsg" : "db assertion failure" , "ok"
        // : 0.0}
        OpReply reply = new OpReply(this);
        reply.addDocument(map(
                entry("assertion",
                        "unauthorized db:"
                                + getDatabaseName()
                                + " lock type:-1 client:"
                                + ((InetSocketAddress) e.getRemoteAddress())
                                        .getAddress().getHostAddress()),
                entry("assertionCode", 10057),
                entry("errmsg", "db assertion failure"), entry("ok", 0.0)));
        return reply;
    }

    private OpReply handleUnauthorizedQuery(MessageEvent e) {
        // { "$err" : "unauthorized db:test lock type:-1 client:127.0.0.1" ,
        // "code" : 10057}
        OpReply reply = new OpReply(this);
        reply.addDocument(map(
                entry("$err",
                        "unauthorized db:"
                                + getDatabaseName()
                                + " lock type:-1 client:"
                                + ((InetSocketAddress) e.getRemoteAddress())
                                        .getAddress().getHostAddress()),
                entry("code", 10057)));
        return reply;
    }

    private OpReply handleAuthFails(OpQuery opQuery) {
        // { "errmsg" : "auth fails" , "ok" : 0.0}
        OpReply reply = new OpReply(opQuery);
        reply.addDocument(map(entry("errmsg", "auth fails"), entry("ok", 0.0)));
        return reply;
    }

    private OpReply handleListCollections(MongoChannelHandler handler,
            String databaseName) {
        logger.info("Handling list collections for database {} ... ",
                databaseName);

        ApplicationInfo application = SubjectUtils.getApplication(Identifier
                .fromName(databaseName));
      
        if (application == null) {
            OpReply reply = new OpReply(this);
            return reply;
        }
       
        EntityManager em = handler.getEmf().getEntityManager(application.getId());
        
        OpReply reply = new OpReply(this);
        
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

    private OpReply handleListUsers() {
        logger.info("Handling list users for database " + getDatabaseName()
                + "... ");

        OpReply reply = new OpReply(this);
        return reply;
    }

    private OpReply handleQuery(MongoChannelHandler handler) {
        logger.info("Handling a query... ");
        ApplicationInfo application = SubjectUtils.getApplication(Identifier
                .fromName(getDatabaseName()));
        if (application == null) {
            OpReply reply = new OpReply(this);
            return reply;
        }
        int count = getNumberToReturn();
        if (count <= 0) {
            count = 30;
        }
        EntityManager em = handler.getEmf().getEntityManager(
                application.getId());
        OpReply reply = new OpReply(this);
        try {
            Results results = null;
            Query q = this.toNativeQuery();
            if (q != null) {
                results = em.searchCollection(em.getApplicationRef(),
                        getCollectionName(), q);
            } else {
                results = em.getCollection(em.getApplicationRef(),
                        getCollectionName(), null, count,
                        Results.Level.ALL_PROPERTIES, false);
            }
            if (!results.isEmpty()) {
                for (Entity entity : results.getEntities()) {
                    reply.addDocument(map(
                            entry("_id", entity.getUuid()),
                            toJsonMap(entity),
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
