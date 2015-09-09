/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence;

import com.google.inject.Injector;
import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.service.StatusService;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.UUID;

/**
 * test status service
 */
public class StatusServiceIT extends AbstractCoreIT {

    private static final Logger LOG = LoggerFactory.getLogger(StatusServiceIT.class);

    public StatusServiceIT() {
        super();
    }
    @Test
    public void testStatus(){
        Injector injector = SpringResource.getInstance().getBean(Injector.class);
        StatusService statusService = injector.getInstance(StatusService.class);
        UUID applicationID =  UUID.randomUUID();
        UUID jobid = UUID.randomUUID();
        //test no status yet
        StatusService.JobStatus status = statusService.getStatus(applicationID, jobid).toBlocking().lastOrDefault(null);
        Assert.assertNull(status);
        statusService.setStatus(applicationID,jobid, StatusService.Status.COMPLETE,null).subscribe();
        //test status no map
        status = statusService.getStatus(applicationID, jobid).toBlocking().lastOrDefault(null);
        Assert.assertNotNull(status);
        Assert.assertEquals(status.getJobStatusId(), jobid);
        Assert.assertEquals(status.getData(), new HashMap<String, Object>());
        Assert.assertEquals(status.getStatus(), StatusService.Status.COMPLETE);

        HashMap<String,Object> map = new HashMap<>();
        map.put("name","hello");
        map.put("count",0);

        statusService.setStatus(applicationID,jobid, StatusService.Status.INPROGRESS,map).subscribe();
        //test status  map
        status = statusService.getStatus(applicationID, jobid).toBlocking().lastOrDefault(null);
        Assert.assertNotNull(status);
        Assert.assertEquals(status.getJobStatusId(), jobid);
        Assert.assertEquals(status.getData(),map);
        Assert.assertEquals(status.getStatus(), StatusService.Status.INPROGRESS);


    }
}
