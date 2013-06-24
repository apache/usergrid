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
package org.usergrid.persistence.exceptions;

import com.github.fge.jsonschema.report.ProcessingReport;

public class InvalidEntitySchemaSyntaxException extends JsonSchemaValidatorException {

	private static final long serialVersionUID = 1L;

	final String entityType;

	public InvalidEntitySchemaSyntaxException(String entityType,
	        ProcessingReport report) {
		super("Schema for " + entityType + " has invalid syntax", report);
		this.entityType = entityType;
	}

	public String getEntityType() {
		return entityType;
	}

}
