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
package org.usergrid.mq;

import static org.usergrid.mq.Queue.getQueueParentPaths;
import static org.usergrid.mq.Queue.normalizeQueuePath;
import static org.usergrid.utils.JsonUtils.mapToFormattedJsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

public class QueuePathsTest {

	private static final Logger logger = LoggerFactory.getLogger(MessagesTest.class);

	public QueuePathsTest() {
	}

	@Test
	public void testPaths() throws Exception {

		logger.info(normalizeQueuePath("a/b/c"));
		logger.info(normalizeQueuePath("a/b/c/"));
		logger.info(normalizeQueuePath("/a/b/c"));
		logger.info(normalizeQueuePath("/////a/b/c"));
		logger.info(normalizeQueuePath("/"));

		logger.info(mapToFormattedJsonString(getQueueParentPaths("/a/b/c")));
		logger.info(mapToFormattedJsonString(getQueueParentPaths("/")));

	}

}
