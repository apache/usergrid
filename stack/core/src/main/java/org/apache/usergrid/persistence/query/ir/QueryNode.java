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
package org.apache.usergrid.persistence.query.ir;


/**
 * The visit the node
 *
 * @author tnine
 */
public abstract class QueryNode {

    /** Visit this node */
    public abstract void visit( NodeVisitor visitor ) throws Exception;


    /**
     * Get the count of the total number of slices in our tree from this node and it's children
     */
    public abstract int getCount();

    /**
     * True if this node should not be used in it's context in the AST, and should ignore it's hint size and always select the max
     * @return
     */
    public abstract boolean ignoreHintSize();
}
