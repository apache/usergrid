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
package org.usergrid.persistence.query.tree;

import static org.junit.Assert.*;

import org.antlr.runtime.CommonToken;
import org.junit.Test;

/**
 * @author tnine
 *
 */
public class LongLiteralTest {

    
    
    /**
     * Test method for {@link org.usergrid.persistence.query.tree.LongLiteral#IntegerLiteral(org.antlr.runtime.Token)}.
     */
    @Test
    public void longMin() {
        
        long value = Long.MIN_VALUE;
        
        String stringVal = String.valueOf(value);
        
        LongLiteral literal = new LongLiteral(new CommonToken(0, stringVal));
        
        assertEquals(value, literal.getValue().longValue());
        
        
    }
    
    
    /**
     * Test method for {@link org.usergrid.persistence.query.tree.LongLiteral#IntegerLiteral(org.antlr.runtime.Token)}.
     */
    @Test
    public void longMax() {
        
        long value = Long.MAX_VALUE;
        
        String stringVal = String.valueOf(value);
        
        LongLiteral literal = new LongLiteral(new CommonToken(0, stringVal));
        
        assertEquals(value, literal.getValue().longValue());
        
        
    }
    
    

}
