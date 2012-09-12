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
package org.usergrid.services.assets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.services.AbstractCollectionService;
import org.usergrid.services.AbstractPathBasedColllectionService;

public class AssetsService extends AbstractPathBasedColllectionService {

	private static final Logger logger = LoggerFactory.getLogger(AssetsService.class);

	public AssetsService() {
		super();
		logger.info("/assets");
	}

}
