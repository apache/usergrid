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
package org.apache.usergrid.persistence.query;


import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(JukitoRunner.class)
@UseModules({ TestIndexModule.class })
public class SingleOrderBySameRangeScanLessConnectionIT extends AbstractIteratingQueryIT {
    @Test
    public void singleOrderBySameRangeScanLessConnection() throws Exception {
        singleOrderBySameRangeScanLessEqual( new ConnectionHelper(app) );
    }
}
