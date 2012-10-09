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
package org.usergrid.persistence;

import static org.usergrid.utils.JsonUtils.mapToFormattedJsonString;

import io.baas.Simple;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.entities.SampleEntity;

public class SchemaTest {

	private static final Logger logger = LoggerFactory
			.getLogger(SchemaTest.class);

	public SchemaTest() {
	}

	@Test
	public void testTypes() throws Exception {

		logger.info(""
				+ Schema.getDefaultSchema().getEntityClass("sample_entity"));
		logger.info(""
				+ Schema.getDefaultSchema().getEntityType(SampleEntity.class));

		SampleEntity entity = new SampleEntity();
		logger.info(entity.getType());
	}
	
	@Test
	public void testThirdPartyEntityTypes() throws Exception {
		String thirdPartyPackage = "io.baas";
		Schema schema = Schema.getDefaultSchema();
		schema.addEntitiesPackage(thirdPartyPackage);
		schema.scanEntities();
		
		List<String> entitiesPackage = schema.getEntitiesPackage();
		for( String entityPackage : entitiesPackage ) {
			logger.info(entityPackage);
		}
		
		Assert.assertEquals(schema.getEntityClass("simple"), Simple.class);
		Assert.assertEquals(schema.getEntityType(Simple.class), "simple");
		
		Simple entity = new Simple();
		logger.info(entity.getType());
	}

	@Test
	public void testSchema() throws Exception {

		dumpSetNames("application");
		dumpSetNames("user");
		dumpSetNames("thing");
	}

	public void dumpSetNames(String entityType) {
		logger.info(entityType + " entity has the following sets: "
				+ Schema.getDefaultSchema().getDictionaryNames(entityType));
	}

	@Test
	public void testJsonSchema() throws Exception {

		logger.info(mapToFormattedJsonString(Schema.getDefaultSchema()
				.getEntityJsonSchema("user")));

		logger.info(mapToFormattedJsonString(Schema.getDefaultSchema()
				.getEntityJsonSchema("test")));
	}

}
