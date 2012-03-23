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
package org.usergrid.persistence.query.ir;


/**
 * @author tnine
 *
 */
public interface NodeVisitor {

     /**
     * 
     * @param node
     * @throws Exception 
     */
    public void visit(AndNode node) throws Exception;
    
    /**
     * 
     * @param node
     * @throws Exception 
     */
    public void visit(NotNode node) throws Exception;
    
    /**
     * 
     * @param node
     * @throws Exception 
     */
    public void visit(OrNode node) throws Exception;
    
    /**
     * 
     * @param node
     * @throws Exception 
     */
    public void visit(SliceNode node) throws Exception;
    
    /**
     * 
     * @param node
     * @throws Exception 
     */
    public void visit(WithinNode node) throws Exception;
}
