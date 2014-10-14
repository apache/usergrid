/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.stack;


import org.apache.usergrid.chop.stack.BasicIpRule;
import org.apache.usergrid.chop.stack.BasicIpRuleSet;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class BasicIpRuleTest {

    private static BasicIpRuleSet ipRuleSet1;
    private static BasicIpRuleSet ipRuleSet2;
    private static BasicIpRuleSet ipRuleSet3;
    private static BasicIpRuleSet ipRuleSet4;
    private static BasicIpRuleSet ipRuleSet5;


    @BeforeClass
    public static void setUp() {
        BasicIpRule ir1 = new BasicIpRule();
        BasicIpRule ir2 = new BasicIpRule();
        BasicIpRule ir3 = new BasicIpRule();
        BasicIpRule ir4 = new BasicIpRule();

        ir1.withFromPort( 5000 )
           .withToPort( 6000 )
           .withIpProtocol( "tcp" )
           .withIpRanges( "0.0.0.0/32" );

        ir2.withFromPort( 80 )
           .withToPort( 80 )
           .withIpProtocol( "tcp" )
           .withIpRanges( "192.168.1.0/24" );


        ir3.withFromPort( 7000 )
           .withToPort( 9000 )
           .withIpProtocol( "tcp" )
           .withIpRanges( "172.16.0.0/16" );

        ir4.withFromPort( 443 )
           .withToPort( 443 )
           .withIpProtocol( "tcp" )
           .withIpRanges( "172.16.0.0/16", "192.168.1.0/24" );

        ipRuleSet1 = new BasicIpRuleSet();
        ipRuleSet2 = new BasicIpRuleSet();
        ipRuleSet3 = new BasicIpRuleSet();
        ipRuleSet4 = new BasicIpRuleSet();
        ipRuleSet5 = new BasicIpRuleSet();

        ipRuleSet1.setName( "test-set-1" );
        ipRuleSet1.addInboundRule( ir1 );
        ipRuleSet1.addInboundRule( ir3 );

        ipRuleSet2.setName( "test-set-1" );
        ipRuleSet2.addInboundRule( ir1 );
        ipRuleSet2.addInboundRule( ir3 );

        ipRuleSet3.setName( "test-set-2" );
        ipRuleSet3.addInboundRule( ir2 );

        ipRuleSet4.setName( "test-set-3" );
        ipRuleSet4.addInboundRule( ir1 );
        ipRuleSet4.addInboundRule( ir3 );

        ipRuleSet5.setName( "test-set-1" );
        ipRuleSet5.addInboundRule( ir4 );
    }


    @Test
    public void testEquals() {
        assertTrue( ipRuleSet1.equals( ipRuleSet2 ) );
    }


    @Test
    public void testNotEquals() {
        assertTrue( ! ipRuleSet1.equals( ipRuleSet4 ) );
        assertTrue( ! ipRuleSet1.equals( ipRuleSet3 ) );
        assertTrue( ! ipRuleSet1.equals( ipRuleSet5 ) );
        assertTrue( ! ipRuleSet3.equals( ipRuleSet5 ) );
    }
}
