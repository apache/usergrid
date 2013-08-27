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
package org.usergrid.persistence.query;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.AbstractCoreIT;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

import java.util.*;

import static org.junit.Assert.*;


/**
 * @author tnine
 * 
 */
public class SingleOrderByComplexIntersectionConnectionIT extends AbstractIteratingQueryIT
{

    @Test
    public void singleOrderByComplexIntersectionConnection() throws Exception {
        singleOrderByComplexIntersection(new ConnectionHelper());
    }
}
